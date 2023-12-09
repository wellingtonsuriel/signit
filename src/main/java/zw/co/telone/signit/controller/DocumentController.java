package zw.co.telone.signit.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import zw.co.telone.signit.email.EmailService;
import zw.co.telone.signit.model.Document;
import zw.co.telone.signit.model.Signature;
import zw.co.telone.signit.repository.DocumentRepository;
import zw.co.telone.signit.repository.SignatureRepository;

import java.util.concurrent.CompletableFuture;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1")
public class DocumentController {
    @Autowired
    private EmailService emailService;




    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    @Autowired
    public DocumentController( DocumentRepository documentRepository, SignatureRepository signatureRepository) {

        this.documentRepository = documentRepository;
        this.signatureRepository= signatureRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdby") String createdby,
            @RequestParam("subject") String subject,
            @RequestParam("signers") List<String> signers
    ) throws Exception {

        // Upload the document to the JAR file
        String fileName = file.getOriginalFilename();
        //String serverPath = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps\\SignitDocs\\" + fileName;
        String serverPath =  "C:\\xampp\\tomcat\\webapps\\SignitDocs\\" + fileName;
        File serverFile = new File(serverPath);
        file.transferTo(serverFile);
        //String serverURL = "file://s-sol-test/SignitDocs/" + fileName;
        String serverURL = "http://172.27.34.80:8080/SignitDocs/" + fileName;

        // Create a new Document instance
        Document document = new Document();
        document.setCreatedby(createdby);
        document.setCreatedon(LocalDate.now());
        document.setSubject(subject);
        document.setDocpath(serverURL);
        document.setSigners(signers);
        document.setSigned(new ArrayList<>());
        document.setAllsigned(0);
        // Save the document to the database
        documentRepository.save(document);

        // Send email notification to the first signer
        String unfilteredUsername = signers.get(0);
        String filteredUsername= unfilteredUsername.replaceAll("\\d", "");

        String unfilteredcreatedby = createdby;
        String filteredcreatedby= unfilteredcreatedby.replaceAll("\\d", "");

        String[] nameParts = filteredcreatedby.split("\\.");
        String formattedName = "";

        for (String part : nameParts) {
            formattedName += part.substring(0, 1).toUpperCase() + part.substring(1) + " ";
        }

        formattedName = formattedName.trim();
        String signerEmail = filteredUsername + "@telone.co.zw";

        // Send email async
        String finalFormattedName = formattedName;
        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendEmail(signerEmail, subject, "I hope this message finds you well. I am writing to " +
                        "request your signature on an important document titled "+ subject+" requested by "+ finalFormattedName +". " +
                        "Please review the document and sign in the designated area at your earliest convenience.\n\n" +"\n" + "\n"+"\n"+
                        "Best Regards,\n\n" +
                        "Signit System");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // Return success message
        return ResponseEntity.ok().body(Map.of("response", "Document Successfully uploaded"));
    }
    @GetMapping("/to-sign/{username}")
    public List<Document> getDocumentsToSign(@PathVariable("username") String username) {
        List<Document> documents = documentRepository.findBySignersContaining(username);
        return documents.stream()
                .filter(document -> document.getSigners().get(0).equals(username))
                .collect(Collectors.toList());
    }

    @GetMapping("/have-signed/{username}")
    public List<Document> getDocumentsHaveSign(@PathVariable("username") String username) {
        List<Document> documents = documentRepository.findBySignedContaining(username);
       return  documents;
    }

    @PutMapping("/sign/{id}")
    public ResponseEntity<Map<String, Object>> signDocument(@PathVariable Long id, @RequestParam("signer") String signer,@RequestParam("file") MultipartFile file) throws IOException {
        // Update the Document instance
        Document document = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
        String fileName = file.getOriginalFilename();
        //String serverPath = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps\\SignitDocs\\" + fileName;
        String serverPath =  "C:\\xampp\\tomcat\\webapps\\SignitDocs\\" + fileName;
        File serverFile = new File(serverPath);
        file.transferTo(serverFile);
        //String serverURL = "file://s-sol-test/SignitDocs/" + fileName;
        String serverURL = "http://172.27.34.80:8080/SignitDocs/" + fileName;
        document.getSigners().remove(signer);
        document.getSigned().add(signer);
        document.setDocpath(serverURL);
        documentRepository.save(document);

        // Send notification email
        if (!document.getSigners().isEmpty()) {




            String unfilteredUsername = document.getSigners().get(0);
            String filteredUsername= unfilteredUsername.replaceAll("\\d", "");

            String createdby = document.getCreatedby();
            String filteredUsername1= createdby.replaceAll("\\d", "");
            String[] nameParts = filteredUsername1.split("\\.");
            String formattedName = "";

            for (String part : nameParts) {
                formattedName += part.substring(0, 1).toUpperCase() + part.substring(1) + " ";
            }

            formattedName = formattedName.trim();
            String nextSigner = filteredUsername + "@telone.co.zw";

            // Send email async
            String finalFormattedName = formattedName;

            //send email
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendEmail(nextSigner, document.getSubject(), "I hope this message finds you well. I am writing to " +
                            "request your signature on an important document titled "+ document.getSubject()+" requested by "+ finalFormattedName+". " +
                            "Please review the document and sign in the designated area at your earliest convenience.\n\n" +
                            "Best Regards,\n\n" +
                            "Signit System");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            document.setAllsigned(1);
            documentRepository.save(document);
            String unfilteredUsername = document.getCreatedby();
            String filteredUsername= unfilteredUsername.replaceAll("\\d", "");
            String createdBy = filteredUsername + "@telone.co.zw";
            // send email
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendEmail(createdBy , document.getSubject(), "Your document" +
                            " titled "+ document.getSubject()+". " +
                            "Have been signed by all signers . You can download it in the system\n\n" +
                            "Best Regards,\n\n" +
                            "Signit System");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }

        return ResponseEntity.ok().body(Map.of("response", "Document signed successfully!"));

    }
    @PutMapping("/addsigners/{id}")
    public ResponseEntity<Map<String, Object>> signDocument(@PathVariable Long id, @RequestParam("signers") List<String> signers) {
        // Update the Document instance
        Document document = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));

        document.getSigners().addAll(signers);
        documentRepository.save(document);

        // Send notification email
        if (!document.getSigners().isEmpty()) {
            String nextSigner = document.getSigners().get(0);
            // EmailService.sendNotificationEmail(nextSigner, document);
        } else {
            document.setAllsigned(1);
            documentRepository.save(document);
            String createdBy = document.getCreatedby();
            // EmailService.sendFullySignedEmail(createdBy, document);
        }
        return ResponseEntity.ok().body(Map.of("response", "Other signers appended succesfully!"));

    }
    @GetMapping("/signeddocs/{createdby}")
    public List<Document> getSignedDocumentsCreatedBy(@PathVariable("createdby") String createdby) {
        return documentRepository.findSignedDocumentsByCreatedBy(createdby);
    }


    @GetMapping("{documentId}")
    public Document downloadDocument(@PathVariable Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId).orElse(null);
        return document;


//        if (document == null) {
//            // Handle the case when the document does not exist
//            return ResponseEntity.notFound().build();
//        }
//
//        File file = new File(document.getDocpath());
//
//        if (!file.exists()) {
//            // Handle the case when the file does not exist
//            return ResponseEntity.notFound().build();
//        }
//
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
//        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
//
//        return ResponseEntity.ok()
//                .headers(headers)
//                .contentLength(file.length())
//                .body(new FileSystemResource(file));
    }
    @PostMapping("/uploadsignature")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username

    ) throws Exception {

        // Upload the signature
        String fileName = file.getOriginalFilename();
       //String serverPath = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps\\SignitSignatures\\" + fileName;
       String serverPath =  "C:\\xampp\\tomcat\\webapps\\SignitSignatures\\" + fileName;
        File serverFile = new File(serverPath);
        file.transferTo(serverFile);
        String serverURL = "http://172.27.34.80:1999/SignitSignatures/" + fileName;
        //String serverURL = "http://127.0.0.1:8080/test/SignitDocs/" + fileName;

        // Create a new signature instance
        Signature signature = new Signature();
        signature.setUsername(username);
        signature.setPath(serverURL);

        // Save the signature to the database
        signatureRepository.save(signature);


        // Return success message
        return ResponseEntity.ok().body(Map.of("response", "Signature Successfully uploaded"));
    }

    @GetMapping("/signature/{username}")
    public Signature getUsersignature(@PathVariable("username") String username) {
        return (Signature) signatureRepository.findByUsername(username);
    }


   @DeleteMapping("/signature/{username}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String username){
        Signature signature = (Signature) signatureRepository.findByUsername(username);
         signatureRepository.delete(signature);
       return ResponseEntity.ok().body(Map.of("response", "Signature deleted Succesfully"));
   }

    @GetMapping("/allsigned")
    public List<Document> allsigneddocs() {
      return  documentRepository.findbysigned();

    }

}