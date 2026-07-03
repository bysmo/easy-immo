package com.easyimmo.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String content) {
        log.info("Préparation de l'envoi d'email à : {} | Sujet : {}", to, subject);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@easy-immo.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email envoyé avec succès à {}", to);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email : {}", e.getMessage());
        }
    }
}
