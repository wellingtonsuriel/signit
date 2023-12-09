package zw.co.telone.signit.email;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
//import microsoft.exchange.webservices.data.core.service.item.MessageBody;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.password}")
    private String senderPassword;

    @Value("${spring.mail.host}")
    private String exchangeServerUrl;

    public void sendEmail(String recipient, String subject, String body) throws Exception {

        ExchangeService exchangeService = new ExchangeService();
        exchangeService.setUrl(URI.create(exchangeServerUrl));

        ExchangeCredentials credentials = new WebCredentials(senderEmail, senderPassword);
        exchangeService.setCredentials(credentials);

        EmailMessage emailMessage = new EmailMessage(exchangeService);
        emailMessage.setSubject(subject);
        emailMessage.setBody(new MessageBody(body));
        emailMessage.getToRecipients().add(recipient);
        emailMessage.send();
    }
}
