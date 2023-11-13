package zw.co.telone.signit.controller;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import zw.co.telone.signit.model.Document;
import zw.co.telone.signit.model.EmailRequest;
import zw.co.telone.signit.repository.DocumentRepository;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {


    private final RestTemplate restTemplate;

    private final DocumentRepository documentRepository;
    @Autowired
    public DocumentController(RestTemplate restTemplate, DocumentRepository documentRepository) {
        this.restTemplate = restTemplate;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdby") String createdby,
            @RequestParam("subject") String subject,
            @RequestParam("signers") List<String> signers
    ) throws IOException {

        // Save the document to the file system
        String fileName = file.getOriginalFilename();
        String filePath = "C:\\Users\\wellington.nhidza\\Documents\\testdoc\\" + fileName;
        file.transferTo(new File(filePath));

        // Create a new Document instance
        Document document = new Document();
        document.setCreatedby(createdby);
        document.setCreatedon(LocalDate.now());
        document.setSubject(subject);
        document.setDocpath(filePath);
        document.setSigners(signers);
        document.setSigned(new ArrayList<>());
        document.setAllsigned(0);

        // Save the document to the database
        documentRepository.save(document);

        // Send email notification to the first signer
        String signerEmail = "wellington.nhidza" + "@telone.co.zw";

        // Send email
        String url = "http://172.27.34.7:1930/mails/api/send";
        EmailRequest email = new EmailRequest();
        email.setTo(signerEmail);
        email.setSubject(subject);
        email.setText("Kindly sign this document");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmailRequest> request = new HttpEntity<>(email, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        // Return success message
        return "Document uploaded successfully";
    }
    @GetMapping("/to-sign/{username}")
    public List<Document> getDocumentsToSign(@PathVariable("username") String username) {
        List<Document> documents = documentRepository.findBySignersContaining(username);
        return documents.stream()
                .filter(document -> document.getSigners().get(0).equals(username))
                .collect(Collectors.toList());
    }


    @PutMapping("/{id}/sign")
    public String signDocument(@PathVariable Long id, @RequestParam("signer") String signer) {
        // Update the Document instance
        Document document = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
        document.getSigners().remove(signer);
        document.getSigned().add(signer);
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

        return "Document signed successfully!";
    }
    @PutMapping("/{id}/addsigners")
    public String signDocument(@PathVariable Long id, @RequestParam("signers") List<String> signers) {
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

        return "Document signed successfully!";
    }
    @GetMapping("/signeddocs/{createdby}")
    public List<Document> getSignedDocumentsCreatedBy(@PathVariable("createdby") String createdby) {
        return documentRepository.findSignedDocumentsByCreatedBy(createdby);
    }


    @GetMapping("{documentId}")
    public ResponseEntity<FileSystemResource> downloadDocument(@PathVariable Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId).orElse(null);

        if (document == null) {
            // Handle the case when the document does not exist
            return ResponseEntity.notFound().build();
        }

        File file = new File(document.getDocpath());

        if (!file.exists()) {
            // Handle the case when the file does not exist
            return ResponseEntity.notFound().build();
        }


        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

}