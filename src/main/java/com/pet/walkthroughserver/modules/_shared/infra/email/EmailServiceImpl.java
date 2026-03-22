package com.pet.walkthroughserver.modules._shared.infra.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Override
    public void sendMail(String to, String subject, String... contents) {

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // Recipient
        try {
            mimeMessage.setRecipient(MimeMessage.RecipientType.TO, InternetAddress.parse(to)[0]);
        } catch (MessagingException e) {
            log.error("Error setting email recipient: {}", e.getMessage());
            throw new RuntimeException("Failed to set email recipient", e);
        }

        // Subject
        try {
            mimeMessage.setSubject(subject);
        } catch (MessagingException e) {
            log.error("Error setting email subject: {}", e.getMessage());
            throw new RuntimeException("Failed to set email subject", e);
        }

        // Body
        try {
            Multipart mimeMultipart = new MimeMultipart();
            for (String content : contents) {
                MimeBodyPart bodyPart = createBodyPart(content);
                mimeMultipart.addBodyPart(bodyPart);
            }
            mimeMessage.setContent(mimeMultipart);
        } catch (MessagingException e) {
            log.error("Error setting email body: {}", e.getMessage());
            throw new RuntimeException("Failed to set email body", e);
        }

        mailSender.send(mimeMessage);
    }

    private MimeBodyPart createBodyPart(String content) throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(content, "utf-8", "html");
        return bodyPart;
    }
}
