package com.easyimmo.billing.controller;

import com.easyimmo.billing.model.SaaSInvoice;
import com.easyimmo.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /**
     * Obtenir les factures SaaS d'une agence (pour les admins de l'agence)
     */
    @GetMapping("/invoices/my-agency")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<Page<SaaSInvoice>> getMyAgencyInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(401).build();
        }
        
        String agencyIdStr = jwt.getClaimAsString("agency_id");
        if (agencyIdStr == null) {
            return ResponseEntity.badRequest().build();
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("periodYear").descending().and(Sort.by("periodMonth").descending()));
        return ResponseEntity.ok(billingService.getAgencyInvoices(UUID.fromString(agencyIdStr), pageable));
    }

    /**
     * Lister toutes les factures SaaS (Super Admin)
     */
    @GetMapping("/invoices")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<Page<SaaSInvoice>> getAllInvoices(
            @RequestParam(required = false) SaaSInvoice.InvoiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(billingService.getAllInvoices(status, pageable));
    }

    /**
     * Marquer une facture SaaS comme payée (manuel par le super admin ou via passerelle)
     */
    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<SaaSInvoice> payInvoice(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        
        String method = body.getOrDefault("paymentMethod", "BANK_TRANSFER");
        return ResponseEntity.ok(billingService.payInvoice(id, method));
    }

    /**
     * Déclencher manuellement la facturation SaaS pour un mois donné (Super Admin)
     */
    @PostMapping("/invoices/trigger")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<Void> triggerInvoices(
            @RequestBody Map<String, Integer> body) {
        
        int month = body.getOrDefault("month", LocalDateNowMonth());
        int year = body.getOrDefault("year", LocalDateNowYear());
        
        billingService.generateInvoicesForPeriod(month, year);
        return ResponseEntity.ok().build();
    }

    private int LocalDateNowMonth() {
        return java.time.LocalDate.now().getMonthValue();
    }

    private int LocalDateNowYear() {
        return java.time.LocalDate.now().getYear();
    }
}
