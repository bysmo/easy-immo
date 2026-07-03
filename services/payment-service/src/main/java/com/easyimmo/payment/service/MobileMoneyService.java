package com.easyimmo.payment.service;

import com.easyimmo.payment.model.*;
import com.easyimmo.payment.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileMoneyService {

    private final MobileMoneyRequestRepository momoRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final DepositPaymentRepository depositPaymentRepository;
    private final RentTransactionRepository rentTransactionRepository;
    private final DepositTransactionRepository depositTransactionRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mtn.momo.subscription-key:}")
    private String mtnSubscriptionKey;

    @Value("${mtn.momo.base-url:https://sandbox.momodeveloper.mtn.com}")
    private String mtnBaseUrl;

    @Value("${orange.money.base-url:}")
    private String orangeBaseUrl;

    /**
     * Initie un débit Mobile Money
     */
    @Transactional
    public Map<String, String> initiateMomoPayment(
            UUID agencyId, String paymentType, UUID targetId, String phone, BigDecimal amount, PaymentMethod provider, String countryCode) {
        
        UUID requestId = UUID.randomUUID();

        // 1. Enregistrer la demande initiale dans notre base
        MobileMoneyRequest momoRequest = MobileMoneyRequest.builder()
            .id(requestId)
            .agencyId(agencyId)
            .provider(provider)
            .countryCode(countryCode)
            .phone(phone)
            .amount(amount.toString())
            .paymentType(paymentType) // RENT or DEPOSIT
            .paymentTargetId(targetId)
            .status(TransactionStatus.PENDING)
            .build();

        momoRepository.save(momoRequest);

        Map<String, String> response = new HashMap<>();
        response.put("requestId", requestId.toString());
        response.put("status", "PENDING");

        // 2. Appel de l'API de l'opérateur en fonction du provider (MTN MoMo ou Orange Money)
        if (provider == PaymentMethod.MTN_MOMO) {
            String externalRef = callMtnRequestToPay(requestId, phone, amount);
            momoRequest.setExternalRef(externalRef);
            momoRepository.save(momoRequest);
            
            response.put("providerRef", externalRef);
            response.put("message", "Demande de paiement MTN MoMo envoyée. Veuillez valider sur votre téléphone.");
        } else if (provider == PaymentMethod.ORANGE_MONEY) {
            String paymentUrl = callOrangeWebPay(requestId, amount);
            response.put("paymentUrl", paymentUrl);
            response.put("message", "Veuillez ouvrir l'URL pour effectuer le paiement Orange Money.");
        }

        return response;
    }

    /**
     * Webhook/Callback appelé par l'opérateur Mobile Money MTN ou Orange pour confirmer le statut
     */
    @Transactional
    public void processCallback(String externalRef, TransactionStatus status, String rawPayload) {
        MobileMoneyRequest momoRequest = momoRepository.findByExternalRef(externalRef)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de paiement introuvable"));

        if (momoRequest.getStatus() != TransactionStatus.PENDING) {
            log.info("Cette demande de paiement {} est déjà traitée", externalRef);
            return;
        }

        momoRequest.setStatus(status);
        momoRequest.setCallbackPayload(rawPayload);
        momoRequest.setCompletedAt(LocalDateTime.now());
        momoRepository.save(momoRequest);

        if (status == TransactionStatus.SUCCESS) {
            BigDecimal amount = new BigDecimal(momoRequest.getAmount());
            
            // Traiter le paiement en fonction du type
            if (momoRequest.getPaymentType().equalsIgnoreCase("RENT")) {
                confirmRentPayment(momoRequest.getAgencyId(), momoRequest.getPaymentTargetId(), amount, momoRequest.getProvider(), externalRef);
            } else if (momoRequest.getPaymentType().equalsIgnoreCase("DEPOSIT")) {
                confirmDepositPayment(momoRequest.getAgencyId(), momoRequest.getPaymentTargetId(), amount, momoRequest.getProvider(), externalRef);
            }
        } else {
            log.warn("Le paiement Mobile Money {} a échoué chez l'opérateur", externalRef);
        }
    }

    private void confirmRentPayment(UUID agencyId, UUID rentPaymentId, BigDecimal amount, PaymentMethod method, String reference) {
        RentPayment rent = rentPaymentRepository.findById(rentPaymentId)
            .orElseThrow(() -> new RuntimeException("Loyer introuvable lors de la confirmation Mobile Money"));

        RentTransaction transaction = RentTransaction.builder()
            .rentPayment(rent)
            .amount(amount)
            .paymentMethod(method)
            .transactionRef(reference)
            .paymentDate(LocalDateTime.now())
            .status(TransactionStatus.SUCCESS)
            .build();

        rentTransactionRepository.save(transaction);

        rent.setPaidAmount(rent.getPaidAmount().add(amount));
        if (rent.getPaidAmount().compareTo(rent.getExpectedAmount().add(rent.getLateFee())) >= 0) {
            rent.setStatus(PaymentStatus.PAID);
        } else {
            rent.setStatus(PaymentStatus.PARTIAL);
        }
        rentPaymentRepository.save(rent);
        log.info("Loyer {} confirmé via Mobile Money", rentPaymentId);
    }

    private void confirmDepositPayment(UUID agencyId, UUID depositPaymentId, BigDecimal amount, PaymentMethod method, String reference) {
        DepositPayment deposit = depositPaymentRepository.findById(depositPaymentId)
            .orElseThrow(() -> new RuntimeException("Caution introuvable lors de la confirmation Mobile Money"));

        DepositTransaction transaction = DepositTransaction.builder()
            .depositPayment(deposit)
            .amount(amount)
            .paymentMethod(method)
            .transactionRef(reference)
            .paymentDate(LocalDateTime.now())
            .status(TransactionStatus.SUCCESS)
            .build();

        depositTransactionRepository.save(transaction);

        deposit.setPaidAmount(deposit.getPaidAmount().add(amount));
        if (deposit.getPaidAmount().compareTo(deposit.getTotalAmount()) >= 0) {
            deposit.setStatus(PaymentStatus.PAID);
        } else {
            deposit.setStatus(PaymentStatus.PARTIAL);
        }
        depositPaymentRepository.save(deposit);
        log.info("Caution {} confirmée via Mobile Money", depositPaymentId);
    }

    /**
     * Simulation/Appel MTN Collection API : Request To Pay
     */
    private String callMtnRequestToPay(UUID requestId, String phone, BigDecimal amount) {
        log.info("Appel API MTN RequestToPay pour le téléphone : {} (Montant: {})", phone, amount);
        
        // Simuler ou faire l'appel réel au portail MTN Momo Sandbox
        // En mode Sandbox/Prod, MTN attend un UUID en Header (X-Reference-Id) pour identifier la transaction.
        String transactionRef = UUID.randomUUID().toString();
        
        try {
            // MTN Sandbox endpoints
            String url = mtnBaseUrl + "/collection/v1_0/requesttopay";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Reference-Id", transactionRef);
            headers.set("X-Target-Environment", "sandbox");
            headers.set("Ocp-Apim-Subscription-Key", mtnSubscriptionKey);
            headers.set("Authorization", "Bearer mock-token-momo"); // Remplacé par token d'authentification OAuth2 MTN

            Map<String, Object> body = new HashMap<>();
            body.put("amount", amount.toString());
            body.put("currency", "XOF");
            body.put("externalId", requestId.toString());
            
            Map<String, String> payer = new HashMap<>();
            payer.put("partyIdType", "MSISDN");
            payer.put("partyId", phone.replaceAll("\\+", "")); // MTN veut le format sans '+'
            body.put("payer", payer);
            
            body.put("payerMessage", "Paiement Easy-Immo");
            body.put("payeeNote", "Easy-Immo");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            // En sandbox, MTN renvoie 202 Accepted.
            // On simule pour les tests locaux si la clé MTN n'est pas configurée
            if (mtnSubscriptionKey == null || mtnSubscriptionKey.isBlank() || mtnSubscriptionKey.contains("your-")) {
                log.info("MTN Subscription Key non configurée. Simulation de transaction réussie.");
                return "MOCK-MTN-" + transactionRef;
            }

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                return transactionRef;
            }
        } catch (Exception e) {
            log.error("Erreur d'appel API MTN MoMo réel : {}. Utilisation du fallback simulé.", e.getMessage());
        }

        return "MOCK-MTN-" + transactionRef;
    }

    /**
     * Simulation/Appel API Orange Money WebPay
     */
    private String callOrangeWebPay(UUID requestId, BigDecimal amount) {
        log.info("Appel API Orange Money WebPay pour requête : {} (Montant: {})", requestId, amount);
        
        // Simuler le lien de redirection Orange Money Webpay pour validation
        // Normalement, Orange renvoie une URL du type: https://webpay.orange-money.orange.com/...
        return "https://sandbox.orange-money-webpay.orange.com/pay?request_id=" + requestId;
    }
}
