package com.easyimmo.lease.controller;

import com.easyimmo.lease.dto.TenantRequest;
import com.easyimmo.lease.dto.TenantResponse;
import com.easyimmo.lease.service.AgencySecurityService;
import com.easyimmo.lease.service.TenantService;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/leases/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final AgencySecurityService securityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody TenantRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tenantService.createTenant(agencyId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<TenantResponse>> getTenants(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(tenantService.getTenants(agencyId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<TenantResponse> getTenant(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(tenantService.getTenant(agencyId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(tenantService.updateTenant(agencyId, id, request));
    }

    /**
     * Endpoint mobile appelé lors de la connexion initiale de l'utilisateur après confirmation OTP par téléphone.
     * Permet d'associer le Keycloak Subject UUID à l'entrée Tenant (saisie par l'agence immobilière via numéro de téléphone).
     */
    @PostMapping("/mobile/register")
    public ResponseEntity<TenantResponse> registerMobileTenant(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String keycloakUserId = jwt.getSubject();
        String phone = body.get("phone");
        String firstName = body.getOrDefault("firstName", jwt.getClaimAsString("given_name"));
        String lastName = body.getOrDefault("lastName", jwt.getClaimAsString("family_name"));
        String email = jwt.getClaimAsString("email");

        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(tenantService.registerMobileTenant(keycloakUserId, phone, firstName, lastName, email));
    }
}
