package com.easyimmo.payment.controller;

import com.easyimmo.payment.model.DepositPayment;
import com.easyimmo.payment.model.OwnerDisbursement;
import com.easyimmo.payment.model.PaymentMethod;
import com.easyimmo.payment.model.RentPayment;
import com.easyimmo.payment.service.AgencySecurityService;
import com.easyimmo.payment.service.MobileMoneyService;
import com.easyimmo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MobileMoneyService momoService;
    private final AgencySecurityService securityService;

    /**
     * Enregistrer un encaissement de caution manuel (Espèces, virement...)
     */
    @PostMapping("/deposits/{id}/record")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<DepositPayment> recordManualDeposit(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        PaymentMethod method = PaymentMethod.valueOf(body.get("paymentMethod").toString());
        String reference = body.getOrDefault("reference", "").toString();

        return ResponseEntity.ok(paymentService.recordManualDepositPayment(agencyId, id, amount, method, reference, agentId));
    }

    /**
     * Enregistrer un encaissement de loyer manuel (Espèces, virement...)
     */
    @PostMapping("/rents/{id}/record")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<RentPayment> recordManualRent(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        PaymentMethod method = PaymentMethod.valueOf(body.get("paymentMethod").toString());
        String reference = body.getOrDefault("reference", "").toString();

        return ResponseEntity.ok(paymentService.recordManualRentPayment(agencyId, id, amount, method, reference, agentId));
    }

    /**
     * Initier un paiement Mobile Money (caution ou loyer)
     * Utile aussi bien pour le portail d'agence que pour l'application mobile locataire
     */
    @PostMapping("/momo/initiate")
    public ResponseEntity<Map<String, String>> initiateMomoPayment(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        
        String paymentType = body.get("paymentType").toString(); // RENT or DEPOSIT
        UUID targetId = UUID.fromString(body.get("targetId").toString());
        String phone = body.get("phone").toString();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        PaymentMethod provider = PaymentMethod.valueOf(body.get("provider").toString()); // MTN_MOMO or ORANGE_MONEY
        String countryCode = body.getOrDefault("countryCode", "BJ").toString();

        return ResponseEntity.ok(momoService.initiateMomoPayment(agencyId, paymentType, targetId, phone, amount, provider, countryCode));
    }

    /**
     * Lister les reversements aux propriétaires
     */
    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<OwnerDisbursement>> getDisbursements(
            @RequestParam(required = false) OwnerDisbursement.DisbursementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(paymentService.getDisbursements(agencyId, status, pageable));
    }

    /**
     * Confirmer le paiement d'un reversement au propriétaire (payout)
     */
    @PostMapping("/disbursements/{id}/pay")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<OwnerDisbursement> payDisbursement(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        PaymentMethod method = PaymentMethod.valueOf(body.getOrDefault("paymentMethod", "CASH"));

        return ResponseEntity.ok(paymentService.payDisbursement(agencyId, id, method, agentId));
    }
}
