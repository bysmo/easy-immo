package com.easyimmo.property.controller;

import com.easyimmo.property.dto.RepairRequest;
import com.easyimmo.property.model.PropertyRepair;
import com.easyimmo.property.service.AgencySecurityService;
import com.easyimmo.property.service.PropertyRepairService;
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
@RequestMapping("/api/properties/repairs")
@RequiredArgsConstructor
public class RepairController {

    private final PropertyRepairService repairService;
    private final AgencySecurityService securityService;

    /**
     * Enregistrer une réparation pour un bien immobilier
     */
    @PostMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyRepair> createRepair(
            @PathVariable UUID propertyId,
            @Valid @RequestBody RepairRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(repairService.createRepair(agencyId, propertyId, request, agentId));
    }

    /**
     * Obtenir les réparations d'un bien particulier
     */
    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<PropertyRepair>> getRepairsByProperty(
            @PathVariable UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(repairService.getRepairsByProperty(agencyId, propertyId, pageable));
    }

    /**
     * Lister toutes les réparations de l'agence
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<PropertyRepair>> getAgencyRepairs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(repairService.getRepairsByAgency(agencyId, pageable));
    }

    /**
     * Mettre à jour une réparation
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyRepair> updateRepair(
            @PathVariable UUID id,
            @Valid @RequestBody RepairRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(repairService.updateRepair(agencyId, id, request));
    }

    /**
     * Supprimer un enregistrement de réparation
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<Void> deleteRepair(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        repairService.deleteRepair(agencyId, id);
        return ResponseEntity.noContent().build();
    }
}
