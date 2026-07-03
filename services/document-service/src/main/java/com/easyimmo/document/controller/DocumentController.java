package com.easyimmo.document.controller;

import com.easyimmo.document.model.Document;
import com.easyimmo.document.service.AgencySecurityService;
import com.easyimmo.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final AgencySecurityService securityService;

    /**
     * Générer un PDF de contrat de bail
     */
    @PostMapping("/leases/{leaseId}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Document> generateLeaseContract(
            @PathVariable UUID leaseId,
            @RequestBody Map<String, Object> data,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        
        return ResponseEntity.ok(documentService.generateLeaseContract(agencyId, leaseId, data, agentId));
    }

    /**
     * Générer une quittance de loyer PDF
     */
    @PostMapping("/rents/{rentPaymentId}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Document> generateRentReceipt(
            @PathVariable UUID rentPaymentId,
            @RequestBody Map<String, Object> data,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        
        return ResponseEntity.ok(documentService.generateRentReceipt(agencyId, rentPaymentId, data, agentId));
    }

    /**
     * Générer un reçu de caution PDF
     */
    @PostMapping("/deposits/{depositPaymentId}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Document> generateDepositReceipt(
            @PathVariable UUID depositPaymentId,
            @RequestBody Map<String, Object> data,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        
        return ResponseEntity.ok(documentService.generateDepositReceipt(agencyId, depositPaymentId, data, agentId));
    }

    /**
     * Récupérer l'URL signée temporaire de téléchargement d'un document
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        String url = documentService.getDownloadUrl(agencyId, id);
        
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Récupérer l'URL de téléchargement signée par Target ID et Type
     */
    @GetMapping("/target/{targetId}")
    public ResponseEntity<Map<String, String>> getDownloadUrlByTarget(
            @PathVariable UUID targetId,
            @RequestParam Document.DocumentType type,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        String url = documentService.getDownloadUrlByTarget(agencyId, targetId, type);
        
        return ResponseEntity.ok(Map.of("url", url));
    }
}
