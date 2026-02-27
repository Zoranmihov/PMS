package PMS.user.Services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {
    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Async("mailExecutor")
    public void sendActivationMail(String to, String token) {
        try {
            String activationUrl = publicBaseUrl + "/api/v1/users/activate?token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Activate your account");

            String textBody = "Activate your account\n\n" +
                    "Open this link to activate:\n" + activationUrl + "\n\n" +
                    "If you didn't request this, ignore the email.\n";

            String htmlBody = "<p>Activate your account</p>" +
                    "<p><a href=\"" + activationUrl + "\">Activate account</a></p>" +
                    "<p>If you didn't request this, ignore the email.</p>";

            helper.setText(textBody, htmlBody);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println(e);
        }
    }
}