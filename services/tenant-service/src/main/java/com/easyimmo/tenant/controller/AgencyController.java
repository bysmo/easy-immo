package com.easyimmo.tenant.controller;

import com.easyimmo.tenant.dto.AgencyCreateRequest;
import com.easyimmo.tenant.dto.AgencyResponse;
import com.easyimmo.tenant.model.Agency;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;

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
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<AgencyResponse> updateAgency(
            @PathVariable UUID id,
            @Valid @RequestBody AgencyCreateRequest request) {
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
     * Stats dashboard admin SaaS
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('SAAS_ADMIN')")
    public ResponseEntity<AgencyService.DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(agencyService.getDashboardStats());
    }
}
