package com.easyimmo.lease.service;

import com.easyimmo.lease.dto.TenantRequest;
import com.easyimmo.lease.dto.TenantResponse;
import com.easyimmo.lease.model.Tenant;
import com.easyimmo.lease.repository.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse createTenant(UUID agencyId, TenantRequest request) {
        if (tenantRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un locataire avec ce numéro de téléphone existe déjà");
        }

        Tenant tenant = Tenant.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .email(request.getEmail())
            .nationalId(request.getNationalId())
            .profession(request.getProfession())
            .emergencyContactName(request.getEmergencyContactName())
            .emergencyContactPhone(request.getEmergencyContactPhone())
            .keycloakUserId(request.getKeycloakUserId())
            .build();

        tenant = tenantRepository.save(tenant);
        log.info("Locataire créé : {} {} (ID: {})", tenant.getFirstName(), tenant.getLastName(), tenant.getId());
        return toResponse(tenant);
    }

    public Page<TenantResponse> getTenants(UUID agencyId, String search, Pageable pageable) {
        Page<Tenant> tenants = (search != null && !search.isBlank())
            ? tenantRepository.searchByAgency(agencyId, search, pageable)
            : tenantRepository.findByAgencyId(agencyId, pageable);
        return tenants.map(this::toResponse);
    }

    public TenantResponse getTenant(UUID agencyId, UUID id) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locataire introuvable"));
        
        // Vérification logique que le locataire a ou a eu un bail avec cette agence
        boolean belongsToAgency = tenant.getLeases().stream()
            .anyMatch(l -> l.getAgencyId().equals(agencyId));
            
        if (!belongsToAgency && tenant.getLeases().size() > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit à ce locataire");
        }

        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(UUID agencyId, UUID id, TenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locataire introuvable"));

        tenant.setFirstName(request.getFirstName());
        tenant.setLastName(request.getLastName());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setNationalId(request.getNationalId());
        tenant.setProfession(request.getProfession());
        tenant.setEmergencyContactName(request.getEmergencyContactName());
        tenant.setEmergencyContactPhone(request.getEmergencyContactPhone());
        
        if (request.getKeycloakUserId() != null) {
            tenant.setKeycloakUserId(request.getKeycloakUserId());
        }

        return toResponse(tenantRepository.save(tenant));
    }

    /**
     * Utilisé lors de l'inscription mobile du locataire.
     * Le locataire s'inscrit par téléphone + OTP. S'il a déjà été enregistré par l'agence avec ce téléphone,
     * on lie son compte Keycloak à son entité Tenant locale dans la base.
     */
    @Transactional
    public TenantResponse registerMobileTenant(String keycloakUserId, String phone, String firstName, String lastName, String email) {
        Tenant tenant = tenantRepository.findByPhone(phone)
            .orElse(null);

        if (tenant != null) {
            // Le locataire a déjà été saisi par l'agence. On associe le compte mobile Keycloak
            tenant.setKeycloakUserId(keycloakUserId);
            if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
                tenant.setEmail(email);
            }
            log.info("Liaison du compte mobile Keycloak {} au locataire existant {}", keycloakUserId, tenant.getId());
        } else {
            // Nouveau locataire s'inscrivant spontanément sans bail préalable pour chercher des maisons
            tenant = Tenant.builder()
                .keycloakUserId(keycloakUserId)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .email(email)
                .build();
            log.info("Inscription d'un nouveau locataire mobile autonome (sans bail actif) : {}", phone);
        }
        
        return toResponse(tenantRepository.save(tenant));
    }

    private TenantResponse toResponse(Tenant tenant) {
        boolean hasActiveLease = tenant.getLeases().stream()
            .anyMatch(l -> l.getStatus() == com.easyimmo.lease.model.Lease.LeaseStatus.ACTIVE);

        return TenantResponse.builder()
            .id(tenant.getId())
            .keycloakUserId(tenant.getKeycloakUserId())
            .firstName(tenant.getFirstName())
            .lastName(tenant.getLastName())
            .fullName(tenant.getFullName())
            .phone(tenant.getPhone())
            .email(tenant.getEmail())
            .nationalId(tenant.getNationalId())
            .profession(tenant.getProfession())
            .emergencyContactName(tenant.getEmergencyContactName())
            .emergencyContactPhone(tenant.getEmergencyContactPhone())
            .hasActiveLease(hasActiveLease)
            .createdAt(tenant.getCreatedAt())
            .build();
    }
}
