package com.easyimmo.payment.service;

import com.easyimmo.payment.model.*;
import com.easyimmo.payment.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final DepositPaymentRepository depositPaymentRepository;
    private final DepositTransactionRepository depositTransactionRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final RentTransactionRepository rentTransactionRepository;
    private final OwnerDisbursementRepository disbursementRepository;

    /**
     * Enregistrer un paiement manuel (Espèces, virement...) pour une caution
     */
    @Transactional
    public DepositPayment recordManualDepositPayment(UUID agencyId, UUID depositPaymentId, BigDecimal amount, PaymentMethod method, String reference, UUID agentId) {
        DepositPayment deposit = depositPaymentRepository.findById(depositPaymentId)
            .filter(d -> d.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caution introuvable"));

        if (deposit.getStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette caution est déjà entièrement réglée");
        }

        BigDecimal remaining = deposit.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant versé dépasse le reste à payer (" + remaining + ")");
        }

        // Créer la transaction de versement
        DepositTransaction transaction = DepositTransaction.builder()
            .depositPayment(deposit)
            .amount(amount)
            .paymentMethod(method)
            .transactionRef(reference)
            .paymentDate(LocalDateTime.now())
            .status(TransactionStatus.SUCCESS)
            .createdBy(agentId)
            .build();

        depositTransactionRepository.save(transaction);

        // Mettre à jour la caution
        deposit.setPaidAmount(deposit.getPaidAmount().add(amount));
        if (deposit.getPaidAmount().compareTo(deposit.getTotalAmount()) >= 0) {
            deposit.setStatus(PaymentStatus.PAID);
        } else {
            deposit.setStatus(PaymentStatus.PARTIAL);
        }

        log.info("Encaissement caution manuel enregistré : {} XOF pour bail {}", amount, deposit.getLeaseId());
        return depositPaymentRepository.save(deposit);
    }

    /**
     * Enregistrer un paiement manuel de loyer (Espèces, virement...)
     */
    @Transactional
    public RentPayment recordManualRentPayment(UUID agencyId, UUID rentPaymentId, BigDecimal amount, PaymentMethod method, String reference, UUID agentId) {
        RentPayment rent = rentPaymentRepository.findById(rentPaymentId)
            .filter(r -> r.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensualité de loyer introuvable"));

        if (rent.getStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce loyer est déjà entièrement réglé");
        }

        BigDecimal remaining = rent.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant versé dépasse le reste à payer (" + remaining + ")");
        }

        // Créer la transaction
        RentTransaction transaction = RentTransaction.builder()
            .rentPayment(rent)
            .amount(amount)
            .paymentMethod(method)
            .transactionRef(reference)
            .paymentDate(LocalDateTime.now())
            .status(TransactionStatus.SUCCESS)
            .createdBy(agentId)
            .build();

        rentTransactionRepository.save(transaction);

        // Mettre à jour le loyer
        rent.setPaidAmount(rent.getPaidAmount().add(amount));
        if (rent.getPaidAmount().compareTo(rent.getExpectedAmount().add(rent.getLateFee())) >= 0) {
            rent.setStatus(PaymentStatus.PAID);
        } else {
            rent.setStatus(PaymentStatus.PARTIAL);
        }

        log.info("Encaissement loyer manuel enregistré : {} XOF (mois {}/{}) pour bail {}", 
            amount, rent.getPeriodMonth(), rent.getPeriodYear(), rent.getLeaseId());
        
        return rentPaymentRepository.save(rent);
    }

    /**
     * Obtenir les reversements attendus pour les propriétaires fonciers
     */
    public Page<OwnerDisbursement> getDisbursements(UUID agencyId, OwnerDisbursement.DisbursementStatus status, Pageable pageable) {
        if (status != null) {
            return disbursementRepository.findByAgencyIdAndStatus(agencyId, status, pageable);
        }
        return disbursementRepository.findByAgencyId(agencyId, pageable);
    }

    /**
     * Valider le reversement à un propriétaire
     */
    @Transactional
    public OwnerDisbursement payDisbursement(UUID agencyId, UUID disbursementId, PaymentMethod method, UUID agentId) {
        OwnerDisbursement disbursement = disbursementRepository.findById(disbursementId)
            .filter(d -> d.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reversement introuvable"));

        if (disbursement.getStatus() == OwnerDisbursement.DisbursementStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce reversement a déjà été effectué");
        }

        disbursement.setStatus(OwnerDisbursement.DisbursementStatus.PAID);
        disbursement.setPaymentMethod(method);
        disbursement.setPaidAt(LocalDateTime.now());
        disbursement.setProcessedBy(agentId);

        log.info("Reversement payé au propriétaire {} pour un montant net de {} XOF", 
            disbursement.getOwnerId(), disbursement.getNetOwnerAmount());
        
        return disbursementRepository.save(disbursement);
    }
}
