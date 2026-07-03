package com.easyimmo.notification.messaging;

import com.easyimmo.notification.event.AgencyCreatedEvent;
import com.easyimmo.notification.event.LeaseCreatedEvent;
import com.easyimmo.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMQListener {

    private final EmailService emailService;

    @RabbitListener(queues = "agency.created.queue")
    public void handleAgencyCreated(AgencyCreatedEvent event) {
        log.info("Événement de création d'agence capté pour l'agence : {}", event.getAgencyName());
        
        String welcomeMessage = String.format(
            "Bonjour %s %s,\n\n" +
            "Bienvenue sur Easy-Immo ! Votre agence immobilière \"%s\" a été enregistrée avec succès.\n" +
            "Vous pouvez dès à présent configurer votre compte administrateur avec votre email : %s.\n\n" +
            "Cordialement,\n" +
            "L'équipe Support Easy-Immo.",
            event.getAdminFirstName(), event.getAdminLastName(),
            event.getAgencyName(), event.getAdminEmail()
        );

        emailService.sendSimpleEmail(
            event.getAdminEmail(),
            "Création de votre compte Agence - Easy-Immo",
            welcomeMessage
        );
    }

    @RabbitListener(queues = "agency.suspended.queue")
    public void handleAgencySuspended(String agencyId) {
        log.info("Événement de suspension capté pour l'agence ID : {}", agencyId);
        // On pourrait interroger tenant-service pour avoir l'email admin, ou juste envoyer une alerte générique
        log.warn("Notification de suspension administrative envoyée pour l'agence {}", agencyId);
    }

    @RabbitListener(queues = "lease.created.queue")
    public void handleLeaseCreated(LeaseCreatedEvent event) {
        log.info("Événement de création de bail capté pour locataire : {}", event.getTenantFullName());

        // Alerte Email si fournie
        String tenantEmail = "no-reply@easy-immo.com"; // Remplacé si event contient l'email
        
        String message = String.format(
            "Bonjour %s,\n\n" +
            "Un contrat de bail a été configuré pour vous sur la plateforme Easy-Immo par votre agence.\n" +
            "Détails du loyer : %s XOF / mois.\n" +
            "Dépôt de caution : %s XOF.\n\n" +
            "Pour suivre vos quittances, régler par Mobile Money et échanger avec votre agence, " +
            "téléchargez notre application mobile Easy-Immo et connectez-vous avec votre numéro : %s.\n\n" +
            "Cordialement,\n" +
            "Le gérant de l'Agence.",
            event.getTenantFullName(),
            event.getMonthlyRent(),
            event.getDepositAmount(),
            event.getTenantPhone()
        );

        // Simulation SMS dans les logs
        log.info("📱 [SMS envoyé au {}] : {}", event.getTenantPhone(), 
            "Easy-Immo: Votre bail est actif. Téléchargez l'app mobile pour payer vos loyers.");

        // Envoi email de confirmation
        emailService.sendSimpleEmail(
            tenantEmail,
            "Votre contrat de bail actif - Easy-Immo",
            message
        );
    }
}
