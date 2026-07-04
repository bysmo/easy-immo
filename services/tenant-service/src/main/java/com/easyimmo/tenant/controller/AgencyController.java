package com.easyimmo.tenant.controller;

import com.easyimmo.tenant.dto.AgencyCreateRequest;
import com.easyimmo.tenant.dto.AgencyUpdateRequest;
import com.easyimmo.tenant.dto.AgencyResponse;
import com.easyimmo.tenant.model.Agency;
import com.easyimmo.tenant.model.SubscriptionPlan;
import com.easyimmo.tenant.service.AgencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.easyimmo.tenant.dto.CollaboratorRequest;
import com.easyimmo.tenant.dto.CollaboratorResponse;
import com.easyimmo.tenant.service.AgencySecurityService;
import org.springframework.security.core.Authentication;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;
    private final AgencySecurityService agencySecurityService;

    /**
     * Créer une agence (admin SaaS seulement)
     */
    @PostMapping("/agencies")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<AgencyResponse> createAgency(
            @Valid @RequestBody AgencyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(agencyService.createAgency(request));
    }

    /**
     * Lister toutes les agences avec pagination et filtres
     */
    @GetMapping("/agencies")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<Page<AgencyResponse>> getAllAgencies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Agency.AgencyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(agencyService.getAllAgencies(search, status, pageable));
    }

    /**
     * Détail d'une agence
     */
    @GetMapping("/agencies/{id}")
    @PreAuthorize("hasRole('SAAS_ADMIN') or " +
                  "(hasRole('AGENCY_ADMIN') and @agencySecurityService.isSameAgency(#id, authentication))")
    public ResponseEntity<AgencyResponse> getAgency(@PathVariable UUID id) {
        return ResponseEntity.ok(agencyService.getAgency(id));
    }

    /**
     * Modifier une agence
     */
    @PutMapping("/agencies/{id}")
    @PreAuthorize("hasRole('SAAS_ADMIN') or " +
                  "(hasRole('AGENCY_ADMIN') and @agencySecurityService.isSameAgency(#id, authentication))")
    public ResponseEntity<AgencyResponse> updateAgency(
            @PathVariable UUID id,
            @Valid @RequestBody AgencyUpdateRequest request) {
        return ResponseEntity.ok(agencyService.updateAgency(id, request));
    }

    /**
     * Suspendre une agence
     */
    @PostMapping("/agencies/{id}/suspend")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<AgencyResponse> suspendAgency(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Suspension administrative");
        return ResponseEntity.ok(agencyService.suspendAgency(id, reason));
    }

    /**
     * Réactiver une agence
     */
    @PostMapping("/agencies/{id}/activate")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<AgencyResponse> activateAgency(@PathVariable UUID id) {
        return ResponseEntity.ok(agencyService.activateAgency(id));
    }

    /**
     * Liste interne des agences actives pour le billing-service (sans auth requis en reseau prive)
     */
    @GetMapping("/agencies/internal/active")
    public ResponseEntity<java.util.List<AgencyResponse>> getActiveAgenciesInternal() {
        return ResponseEntity.ok(agencyService.getActiveAgenciesInternal());
    }

    /**
     * Liste des plans d'abonnement actifs
     */
    @GetMapping("/plans")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<java.util.List<SubscriptionPlan>> getActivePlans() {
        return ResponseEntity.ok(agencyService.getActivePlans());
    }

    /**
     * Stats dashboard admin SaaS
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<AgencyService.DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(agencyService.getDashboardStats());
    }

    /**
     * Lister les collaborateurs de l'agence de l'utilisateur connecté
     */
    @GetMapping("/agencies/collaborators")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<java.util.List<CollaboratorResponse>> getCollaborators(Authentication authentication) {
        UUID agencyId = agencySecurityService.getAgencyId(authentication);
        return ResponseEntity.ok(agencyService.getCollaborators(agencyId));
    }

    /**
     * Créer/inviter un collaborateur dans l'agence de l'utilisateur connecté
     */
    @PostMapping("/agencies/collaborators")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<CollaboratorResponse> createCollaborator(
            @Valid @RequestBody CollaboratorRequest request,
            Authentication authentication) {
        UUID agencyId = agencySecurityService.getAgencyId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(agencyService.createCollaborator(agencyId, request));
    }
}
