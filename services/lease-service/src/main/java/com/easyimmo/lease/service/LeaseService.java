package com.easyimmo.lease.service;

import com.easyimmo.lease.client.PropertyClient;
import com.easyimmo.lease.dto.*;
import com.easyimmo.lease.event.LeaseCreatedEvent;
import com.easyimmo.lease.model.Lease;
import com.easyimmo.lease.model.LeaseRenewal;
import com.easyimmo.lease.model.LeaseTermination;
import com.easyimmo.lease.model.Tenant;
import com.easyimmo.lease.repository.LeaseRenewalRepository;
import com.easyimmo.lease.repository.LeaseRepository;
import com.easyimmo.lease.repository.LeaseTerminationRepository;
import com.easyimmo.lease.repository.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final LeaseRenewalRepository renewalRepository;
    private final LeaseTerminationRepository terminationRepository;
    private final PropertyClient propertyClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public LeaseResponse createLease(UUID agencyId, LeaseRequest request, UUID agentId) {
        // 1. Récupérer le locataire
        Tenant tenant = tenantRepository.findById(request.getTenantId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locataire introuvable"));

        // 2. Récupérer le bien via Feign (vérifier existence et disponibilité)
        PropertyClient.PropertyDto property;
        try {
            property = propertyClient.getPropertyById(request.getPropertyId());
        } catch (Exception e) {
            log.error("Erreur appel PROPERTY-SERVICE: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le bien immobilier spécifié n'existe pas");
        }

        if (!property.getStatus().equalsIgnoreCase("AVAILABLE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce bien n'est pas disponible à la location");
        }

        // 3. Créer le bail
        Lease lease = Lease.builder()
            .agencyId(agencyId)
            .propertyId(request.getPropertyId())
            .tenant(tenant)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .monthlyRent(request.getMonthlyRent())
            .depositAmount(request.getDepositAmount())
            .paymentDay(request.getPaymentDay())
            .status(Lease.LeaseStatus.ACTIVE)
            .notes(request.getNotes())
            .build();

        lease = leaseRepository.save(lease);

        // 4. Mettre à jour le statut du bien en OCCUPIED
        try {
            propertyClient.updatePropertyStatus(property.getId(), "OCCUPIED");
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut du bien: {}", e.getMessage());
            // Dans une vraie archi de microservices de prod on gèrerait une compensation transactionnelle (Saga)
        }

        // 5. Publier l'événement RabbitMQ (création caution + loyers + notifications)
        LeaseCreatedEvent event = LeaseCreatedEvent.builder()
            .leaseId(lease.getId())
            .agencyId(agencyId)
            .propertyId(lease.getPropertyId())
            .tenantId(tenant.getId())
            .tenantPhone(tenant.getPhone())
            .tenantFullName(tenant.getFullName())
            .startDate(lease.getStartDate())
            .monthlyRent(lease.getMonthlyRent())
            .depositAmount(lease.getDepositAmount())
            .build();

        rabbitTemplate.convertAndSend("easy-immo.exchange", "lease.created", event);
        log.info("Bail créé : {} pour agence {} et locataire {}", lease.getId(), agencyId, tenant.getId());

        return toResponse(lease, property);
    }

    public Page<LeaseResponse> getLeases(UUID agencyId, String search, Lease.LeaseStatus status, Pageable pageable) {
        Page<Lease> leases;
        if (search != null && !search.isBlank()) {
            leases = leaseRepository.searchByAgency(agencyId, search, pageable);
        } else if (status != null) {
            leases = leaseRepository.findByAgencyIdAndStatus(agencyId, status, pageable);
        } else {
            leases = leaseRepository.findByAgencyId(agencyId, pageable);
        }
        return leases.map(lease -> {
            PropertyClient.PropertyDto property = null;
            try {
                property = propertyClient.getPropertyById(lease.getPropertyId());
            } catch (Exception ignored) {}
            return toResponse(lease, property);
        });
    }

    public LeaseResponse getLease(UUID agencyId, UUID leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
            .filter(l -> l.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat de bail introuvable"));

        PropertyClient.PropertyDto property = null;
        try {
            property = propertyClient.getPropertyById(lease.getPropertyId());
        } catch (Exception ignored) {}

        return toResponse(lease, property);
    }

    @Transactional
    public LeaseResponse renewLease(UUID agencyId, UUID leaseId, RenewalRequest request, UUID agentId) {
        Lease lease = leaseRepository.findById(leaseId)
            .filter(l -> l.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat de bail introuvable"));

        if (lease.getStatus() != Lease.LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un bail actif peut être renouvelé");
        }

        LeaseRenewal renewal = LeaseRenewal.builder()
            .lease(lease)
            .newEndDate(request.getNewEndDate())
            .newMonthlyRent(request.getNewMonthlyRent())
            .notes(request.getNotes())
            .createdBy(agentId)
            .build();

        renewalRepository.save(renewal);

        lease.setEndDate(request.getNewEndDate());
        
        // Mettre à jour le loyer si modifié
        if (request.getNewMonthlyRent().compareTo(lease.getMonthlyRent()) != 0) {
            lease.setMonthlyRent(request.getNewMonthlyRent());
            // On pourrait publier un event de changement de loyer si nécessaire
        }

        lease = leaseRepository.save(lease);
        log.info("Bail renouvelé : {}, nouveau loyer : {}", leaseId, lease.getMonthlyRent());

        PropertyClient.PropertyDto property = null;
        try {
            property = propertyClient.getPropertyById(lease.getPropertyId());
        } catch (Exception ignored) {}

        return toResponse(lease, property);
    }

    @Transactional
    public LeaseResponse terminateLease(UUID agencyId, UUID leaseId, TerminationRequest request, UUID agentId) {
        Lease lease = leaseRepository.findById(leaseId)
            .filter(l -> l.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat de bail introuvable"));

        if (lease.getStatus() != Lease.LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un bail actif peut être résilié");
        }

        LeaseTermination termination = LeaseTermination.builder()
            .lease(lease)
            .terminationDate(request.getTerminationDate())
            .reason(request.getReason())
            .depositRefunded(request.getDepositRefunded())
            .notes(request.getNotes())
            .createdBy(agentId)
            .build();

        terminationRepository.save(termination);

        lease.setStatus(Lease.LeaseStatus.TERMINATED);
        lease = leaseRepository.save(lease);

        // Remettre le bien en AVAILABLE
        try {
            propertyClient.updatePropertyStatus(lease.getPropertyId(), "AVAILABLE");
        } catch (Exception e) {
            log.error("Erreur lors de la remise en disponibilité du bien: {}", e.getMessage());
        }

        log.info("Bail résilié : {}, date de fin : {}", leaseId, request.getTerminationDate());

        PropertyClient.PropertyDto property = null;
        try {
            property = propertyClient.getPropertyById(lease.getPropertyId());
        } catch (Exception ignored) {}

        return toResponse(lease, property);
    }

    /**
     * Pour les locataires connectés sur mobile - obtenir leur bail actif
     */
    public LeaseResponse getActiveLeaseForMobileTenant(String keycloakUserId) {
        Lease lease = leaseRepository.findActiveLeaseByKeycloakUserId(keycloakUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun bail actif trouvé"));

        PropertyClient.PropertyDto property = null;
        try {
            property = propertyClient.getPropertyById(lease.getPropertyId());
        } catch (Exception ignored) {}

        return toResponse(lease, property);
    }

    /**
     * Historique des locations passées du locataire mobile connecté
     */
    public List<LeaseResponse> getAllLeasesForMobileTenant(String keycloakUserId) {
        List<Lease> leases = leaseRepository.findAllLeasesByKeycloakUserId(keycloakUserId);
        return leases.stream()
            .map(lease -> {
                PropertyClient.PropertyDto property = null;
                try {
                    property = propertyClient.getPropertyById(lease.getPropertyId());
                } catch (Exception ignored) {}
                return toResponse(lease, property);
            })
            .collect(Collectors.toList());
    }

    private LeaseResponse toResponse(Lease lease, PropertyClient.PropertyDto property) {
        return LeaseResponse.builder()
            .id(lease.getId())
            .agencyId(lease.getAgencyId())
            .propertyId(lease.getPropertyId())
            .propertyReference(property != null ? property.getReference() : null)
            .propertyAddress(property != null ? property.getAddress() : null)
            .propertyCity(property != null ? property.getCity() : null)
            .tenantId(lease.getTenant().getId())
            .tenantFullName(lease.getTenant().getFullName())
            .tenantPhone(lease.getTenant().getPhone())
            .tenantEmail(lease.getTenant().getEmail())
            .startDate(lease.getStartDate())
            .endDate(lease.getEndDate())
            .monthlyRent(lease.getMonthlyRent())
            .depositAmount(lease.getDepositAmount())
            .paymentDay(lease.getPaymentDay())
            .status(lease.getStatus())
            .contractPdfUrl(lease.getContractPdfUrl())
            .notes(lease.getNotes())
            .createdAt(lease.getCreatedAt())
            .build();
    }
}
