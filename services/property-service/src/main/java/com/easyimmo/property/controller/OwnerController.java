package com.easyimmo.property.controller;

import com.easyimmo.property.dto.OwnerRequest;
import com.easyimmo.property.dto.OwnerResponse;
import com.easyimmo.property.service.AgencySecurityService;
import com.easyimmo.property.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/properties/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;
    private final AgencySecurityService securityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<OwnerResponse> createOwner(
            @Valid @RequestBody OwnerRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ownerService.createOwner(agencyId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<OwnerResponse>> getOwners(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(ownerService.getOwners(agencyId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<OwnerResponse> getOwner(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(ownerService.getOwner(agencyId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<OwnerResponse> updateOwner(
            @PathVariable UUID id,
            @Valid @RequestBody OwnerRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(ownerService.updateOwner(agencyId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<Void> deleteOwner(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        ownerService.deleteOwner(agencyId, id);
        return ResponseEntity.noContent().build();
    }
}
