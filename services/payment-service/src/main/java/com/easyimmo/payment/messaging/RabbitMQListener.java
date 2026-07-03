package com.easyimmo.payment.messaging;

import com.easyimmo.payment.event.LeaseCreatedEvent;
import com.easyimmo.payment.model.DepositPayment;
import com.easyimmo.payment.model.PaymentStatus;
import com.easyimmo.payment.model.RentPayment;
import com.easyimmo.payment.repository.DepositPaymentRepository;
import com.easyimmo.payment.repository.RentPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMQListener {

    private final DepositPaymentRepository depositPaymentRepository;
    private final RentPaymentRepository rentPaymentRepository;

    @RabbitListener(queues = "lease.created.queue")
    public void handleLeaseCreated(LeaseCreatedEvent event) {
        log.info("Message reçu - Nouveau bail créé : {}", event.getLeaseId());

        // 1. Initialiser le paiement de la caution si supérieure à 0
        if (event.getDepositAmount() != null && event.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositPayment deposit = DepositPayment.builder()
                .agencyId(event.getAgencyId())
                .leaseId(event.getLeaseId())
                .totalAmount(event.getDepositAmount())
                .paidAmount(BigDecimal.ZERO)
                .status(PaymentStatus.PENDING)
                .build();
            depositPaymentRepository.save(deposit);
            log.info("Caution initialisée pour le bail {} : {} XOF", event.getLeaseId(), event.getDepositAmount());
        }

        // 2. Initialiser le premier mois de loyer à échoir
        LocalDate now = LocalDate.now();
        RentPayment rent = RentPayment.builder()
            .agencyId(event.getAgencyId())
            .leaseId(event.getLeaseId())
            .periodMonth(now.getMonthValue())
            .periodYear(now.getYear())
            .expectedAmount(event.getMonthlyRent())
            .paidAmount(BigDecimal.ZERO)
            .dueDate(now.withDayOfMonth(5)) // Échéance fixée par défaut au 5
            .status(PaymentStatus.PENDING)
            .build();
        
        rentPaymentRepository.save(rent);
        log.info("Premier loyer initialisé (mois {}/{}) pour le bail {} : {} XOF", 
            now.getMonthValue(), now.getYear(), event.getLeaseId(), event.getMonthlyRent());
    }
}
