package com.easyimmo.lease.controller;

import com.easyimmo.lease.dto.LeaseRequest;
import com.easyimmo.lease.dto.LeaseResponse;
import com.easyimmo.lease.dto.RenewalRequest;
import com.easyimmo.lease.dto.TerminationRequest;
import com.easyimmo.lease.model.Lease;
import com.easyimmo.lease.service.AgencySecurityService;
import com.easyimmo.lease.service.LeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;
    private final AgencySecurityService securityService;

    /**
     * Créer un bail (affectation d'un logement à un locataire)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<LeaseResponse> createLease(
            @Valid @RequestBody LeaseRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(leaseService.createLease(agencyId, request, agentId));
    }

    /**
     * Lister les baux de l'agence
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<LeaseResponse>> getAgencyLeases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Lease.LeaseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(leaseService.getLeases(agencyId, search, status, pageable));
    }

    /**
     * Obtenir le détail d'un bail
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<LeaseResponse> getLease(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(leaseService.getLease(agencyId, id));
    }

    /**
     * Renouveler un bail (avenant)
     */
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<LeaseResponse> renewLease(
            @PathVariable UUID id,
            @Valid @RequestBody RenewalRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        return ResponseEntity.ok(leaseService.renewLease(agencyId, id, request, agentId));
    }

    /**
     * Résilier un bail (donner congé)
     */
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<LeaseResponse> terminateLease(
            @PathVariable UUID id,
            @Valid @RequestBody TerminationRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        return ResponseEntity.ok(leaseService.terminateLease(agencyId, id, request, agentId));
    }

    // ─── ENDPOINTS MOBILE (LOCATAIRE CONNECTÉ) ───────────────────────────

    /**
     * Obtenir le bail en cours (actif) de l'utilisateur connecté sur l'application mobile
     */
    @GetMapping("/mobile/active")
    public ResponseEntity<LeaseResponse> getActiveLeaseForMobileTenant(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(leaseService.getActiveLeaseForMobileTenant(jwt.getSubject()));
    }

    /**
     * Obtenir tout l'historique des baux de l'utilisateur connecté sur l'application mobile
     */
    @GetMapping("/mobile/history")
    public ResponseEntity<List<LeaseResponse>> getAllLeasesForMobileTenant(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(leaseService.getAllLeasesForMobileTenant(jwt.getSubject()));
    }
}
