package com.easyimmo.property.controller;

import com.easyimmo.property.dto.PropertyRequest;
import com.easyimmo.property.dto.PropertyResponse;
import com.easyimmo.property.model.Property;
import com.easyimmo.property.service.AgencySecurityService;
import com.easyimmo.property.service.PropertyService;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final AgencySecurityService securityService;

    /**
     * Recherche publique (accessible aux clients / locataires via mobile sans connexion ou avec)
     */
    @GetMapping("/public/search")
    public ResponseEntity<Page<PropertyResponse>> searchAvailableProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) Property.PropertyType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(propertyService.getAvailablePropertiesPublic(city, minRent, maxRent, type, search, pageable));
    }

    /**
     * Créer un bien immobilier
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyResponse> createProperty(
            @Valid @RequestBody PropertyRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(propertyService.createProperty(agencyId, request));
    }

    /**
     * Lister les biens de l'agence (avec filtres de statut et recherche textuelle)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Page<PropertyResponse>> getAgencyProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Property.PropertyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        UUID agencyId = securityService.getAgencyId(authentication);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(propertyService.getProperties(agencyId, search, status, pageable));
    }

    /**
     * Détail d'un bien
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyResponse> getProperty(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(propertyService.getProperty(agencyId, id));
    }

    /**
     * Modifier un bien
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody PropertyRequest request,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(propertyService.updateProperty(agencyId, id, request));
    }

    /**
     * Supprimer un bien
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENCY_ADMIN')")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        propertyService.deleteProperty(agencyId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Uploader une photo de bien
     */
    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyResponse> uploadPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isCover,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(propertyService.uploadPhoto(agencyId, id, file, isCover));
    }

    /**
     * Supprimer une photo de bien
     */
    @DeleteMapping("/{id}/photos/{photoId}")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyResponse> deletePhoto(
            @PathVariable UUID id,
            @PathVariable UUID photoId,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        return ResponseEntity.ok(propertyService.deletePhoto(agencyId, id, photoId));
    }

    /**
     * Mettre à jour le loyer (avec archivage historique)
     */
    @PostMapping("/{id}/rent")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<PropertyResponse> updateRent(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        UUID agencyId = securityService.getAgencyId(authentication);
        UUID agentId = securityService.getUserId(authentication);
        
        BigDecimal newRent = new BigDecimal(body.get("rent").toString());
        String reason = body.getOrDefault("reason", "Ajustement de loyer").toString();
        
        return ResponseEntity.ok(propertyService.updateRent(agencyId, id, newRent, reason, agentId));
    }

    /**
     * Mettre à jour le statut du bien (AVAILABLE/OCCUPIED...)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENCY_ADMIN', 'AGENCY_AGENT')")
    public ResponseEntity<Void> updatePropertyStatus(
            @PathVariable UUID id,
            @RequestParam Property.PropertyStatus status) {
        propertyService.updatePropertyStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
