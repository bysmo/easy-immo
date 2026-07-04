package com.easyimmo.tenant.service;

import com.easyimmo.tenant.dto.AgencyCreateRequest;
import com.easyimmo.tenant.dto.AgencyUpdateRequest;
import com.easyimmo.tenant.dto.AgencyResponse;
import com.easyimmo.tenant.event.AgencyCreatedEvent;
import com.easyimmo.tenant.model.Agency;
import com.easyimmo.tenant.model.AgencySubscription;
import com.easyimmo.tenant.model.SubscriptionPlan;
import com.easyimmo.tenant.repository.AgencyRepository;
import com.easyimmo.tenant.repository.AgencySubscriptionRepository;
import com.easyimmo.tenant.repository.SubscriptionPlanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.easyimmo.tenant.model.Collaborator;
import com.easyimmo.tenant.repository.CollaboratorRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final SubscriptionPlanRepository planRepository;
    private final AgencySubscriptionRepository subscriptionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final KeycloakService keycloakService;
    private final CollaboratorRepository collaboratorRepository;

    @Transactional
    public AgencyResponse createAgency(AgencyCreateRequest request) {
        // Vérifier unicité email
        if (agencyRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Une agence avec cet email existe déjà");
        }

        // Récupérer le plan si fourni
        SubscriptionPlan plan = null;
        if (request.getSubscriptionPlanId() != null) {
            plan = planRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Plan tarifaire introuvable"));
        } else {
            // Plan par défaut : STARTER
            plan = planRepository.findByName("STARTER")
                .orElse(null);
        }

        // Créer l'agence
        Agency agency = Agency.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .address(request.getAddress())
            .city(request.getCity())
            .country(request.getCountry())
            .subscriptionPlan(plan)
            .status(Agency.AgencyStatus.TRIAL)
            .trialEndsAt(LocalDateTime.now().plusDays(30)) // 30 jours d'essai
            .build();

        agency = agencyRepository.save(agency);

        // Créer l'abonnement
        if (plan != null) {
            AgencySubscription subscription = AgencySubscription.builder()
                .agency(agency)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(AgencySubscription.SubscriptionStatus.ACTIVE)
                .autoRenew(true)
                .build();
            subscriptionRepository.save(subscription);
        }

        // 1. Créer l'admin de l'agence dans Keycloak
        String keycloakUserId = keycloakService.createUser(
            request.getAdminUsername(),
            request.getAdminEmail(),
            request.getAdminFirstName(),
            request.getAdminLastName(),
            request.getAdminPassword(),
            agency.getId().toString(),
            "AGENCY_ADMIN"
        );

        // 2. Enregistrer l'admin localement comme Collaborator
        Collaborator adminCollaborator = Collaborator.builder()
            .keycloakUserId(keycloakUserId)
            .agency(agency)
            .firstName(request.getAdminFirstName())
            .lastName(request.getAdminLastName())
            .email(request.getAdminEmail())
            .phone(request.getAdminPhone())
            .role("AGENCY_ADMIN")
            .status(Collaborator.CollaboratorStatus.ACTIVE)
            .build();
        collaboratorRepository.save(adminCollaborator);

        // Publier l'event sur RabbitMQ (pour notifier et envoyer l'email de bienvenue)
        AgencyCreatedEvent event = AgencyCreatedEvent.builder()
            .agencyId(agency.getId())
            .agencyName(agency.getName())
            .agencyEmail(agency.getEmail())
            .adminFirstName(request.getAdminFirstName())
            .adminLastName(request.getAdminLastName())
            .adminEmail(request.getAdminEmail())
            .adminPhone(request.getAdminPhone())
            .adminUsername(request.getAdminUsername())
            .adminPassword(request.getAdminPassword())
            .build();

        rabbitTemplate.convertAndSend(
            "easy-immo.exchange",
            "agency.created",
            event
        );

        log.info("Agence créée : {} ({}) avec son administrateur Keycloak ID {}", agency.getName(), agency.getId(), keycloakUserId);
        return toResponse(agency);
    }

    public Page<AgencyResponse> getAllAgencies(String search, Agency.AgencyStatus status, Pageable pageable) {
        Page<Agency> agencies;
        if (search != null && !search.isBlank()) {
            agencies = agencyRepository.searchAgencies(search, pageable);
        } else if (status != null) {
            agencies = agencyRepository.findByStatus(status, pageable);
        } else {
            agencies = agencyRepository.findAll(pageable);
        }
        return agencies.map(this::toResponse);
    }

    public AgencyResponse getAgency(UUID id) {
        Agency agency = agencyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));
        return toResponse(agency);
    }

    @Transactional
    public AgencyResponse updateAgency(UUID id, AgencyUpdateRequest request) {
        Agency agency = agencyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));

        agency.setName(request.getName());
        agency.setPhone(request.getPhone());
        agency.setAddress(request.getAddress());
        agency.setCity(request.getCity());
        agency.setCountry(request.getCountry());

        return toResponse(agencyRepository.save(agency));
    }

    @Transactional
    public AgencyResponse suspendAgency(UUID id, String reason) {
        Agency agency = agencyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));

        agency.setStatus(Agency.AgencyStatus.SUSPENDED);
        agency.setSuspendedAt(LocalDateTime.now());
        agency.setSuspensionReason(reason);

        // Publier event pour notifier et bloquer les accès
        rabbitTemplate.convertAndSend(
            "easy-immo.exchange",
            "agency.suspended",
            agency.getId().toString()
        );

        log.info("Agence suspendue : {} - Raison: {}", id, reason);
        return toResponse(agencyRepository.save(agency));
    }

    @Transactional
    public AgencyResponse activateAgency(UUID id) {
        Agency agency = agencyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));

        agency.setStatus(Agency.AgencyStatus.ACTIVE);
        agency.setSuspendedAt(null);
        agency.setSuspensionReason(null);

        log.info("Agence activée : {}", id);
        return toResponse(agencyRepository.save(agency));
    }

    public DashboardStats getDashboardStats() {
        return DashboardStats.builder()
            .totalAgencies(agencyRepository.count())
            .activeAgencies(agencyRepository.countByStatus(Agency.AgencyStatus.ACTIVE))
            .trialAgencies(agencyRepository.countByStatus(Agency.AgencyStatus.TRIAL))
            .suspendedAgencies(agencyRepository.countByStatus(Agency.AgencyStatus.SUSPENDED))
            .build();
    }

    private AgencyResponse toResponse(Agency agency) {
        return AgencyResponse.builder()
            .id(agency.getId())
            .name(agency.getName())
            .logoUrl(agency.getLogoUrl())
            .address(agency.getAddress())
            .city(agency.getCity())
            .country(agency.getCountry())
            .phone(agency.getPhone())
            .email(agency.getEmail())
            .status(agency.getStatus())
            .subscriptionPlanName(agency.getSubscriptionPlan() != null
                ? agency.getSubscriptionPlan().getName() : null)
            .subscriptionPriceMonthly(agency.getSubscriptionPlan() != null
                ? agency.getSubscriptionPlan().getPriceMonthly() : null)
            .trialEndsAt(agency.getTrialEndsAt())
            .createdAt(agency.getCreatedAt())
            .build();
    }

    public java.util.List<AgencyResponse> getActiveAgenciesInternal() {
        java.util.List<Agency> activeAgencies = agencyRepository.findByStatusIn(
            java.util.List.of(Agency.AgencyStatus.ACTIVE, Agency.AgencyStatus.TRIAL)
        );
        return activeAgencies.stream()
            .map(this::toResponse)
            .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<SubscriptionPlan> getActivePlans() {
        return planRepository.findByIsActiveTrue();
    }

    public java.util.List<com.easyimmo.tenant.dto.CollaboratorResponse> getCollaborators(UUID agencyId) {
        return collaboratorRepository.findByAgencyId(agencyId).stream()
            .map(this::toCollaboratorResponse)
            .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public com.easyimmo.tenant.dto.CollaboratorResponse createCollaborator(UUID agencyId, com.easyimmo.tenant.dto.CollaboratorRequest request) {
        // Vérifier unicité email
        if (collaboratorRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un collaborateur avec cet email existe déjà");
        }

        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));

        // Vérifier la limite d'agents du plan de l'agence
        if (agency.getSubscriptionPlan() != null && agency.getSubscriptionPlan().getMaxAgents() != -1) {
            long currentAgents = collaboratorRepository.findByAgencyId(agencyId).size();
            if (currentAgents >= agency.getSubscriptionPlan().getMaxAgents()) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Limite d'agents atteinte pour votre abonnement (" + agency.getSubscriptionPlan().getMaxAgents() + ")");
            }
        }

        // 1. Créer l'agent dans Keycloak
        String keycloakUserId = keycloakService.createUser(
            request.getUsername(),
            request.getEmail(),
            request.getFirstName(),
            request.getLastName(),
            request.getPassword(),
            agencyId.toString(),
            request.getRole()
        );

        // 2. Créer l'agent localement
        Collaborator collaborator = Collaborator.builder()
            .keycloakUserId(keycloakUserId)
            .agency(agency)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .role(request.getRole())
            .status(Collaborator.CollaboratorStatus.ACTIVE)
            .build();

        collaborator = collaboratorRepository.save(collaborator);

        log.info("Collaborateur/agent {} créé pour l'agence {}", collaborator.getEmail(), agencyId);
        return toCollaboratorResponse(collaborator);
    }

    private com.easyimmo.tenant.dto.CollaboratorResponse toCollaboratorResponse(Collaborator collaborator) {
        return com.easyimmo.tenant.dto.CollaboratorResponse.builder()
            .id(collaborator.getId())
            .keycloakUserId(collaborator.getKeycloakUserId())
            .agencyId(collaborator.getAgency().getId())
            .firstName(collaborator.getFirstName())
            .lastName(collaborator.getLastName())
            .fullName(collaborator.getFirstName() + " " + collaborator.getLastName())
            .email(collaborator.getEmail())
            .phone(collaborator.getPhone())
            .role(collaborator.getRole())
            .status(collaborator.getStatus().name())
            .createdAt(collaborator.getCreatedAt())
            .build();
    }

    // Classe interne stats
    @lombok.Data
    @lombok.Builder
    public static class DashboardStats {
        private long totalAgencies;
        private long activeAgencies;
        private long trialAgencies;
        private long suspendedAgencies;
    }
}
