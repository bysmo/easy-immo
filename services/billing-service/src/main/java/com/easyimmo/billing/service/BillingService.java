package com.easyimmo.billing.service;

import com.easyimmo.billing.client.TenantClient;
import com.easyimmo.billing.model.SaaSInvoice;
import com.easyimmo.billing.repository.SaaSInvoiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final SaaSInvoiceRepository invoiceRepository;
    private final TenantClient tenantClient;

    /**
     * Tâche planifiée : S'exécute le 1er de chaque mois à 1h du matin
     * Génère automatiquement les factures SaaS des agences actives
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void generateMonthlyInvoicesScheduled() {
        log.info("Lancement de la génération automatique des factures SaaS mensuelles...");
        LocalDate now = LocalDate.now();
        generateInvoicesForPeriod(now.getMonthValue(), now.getYear());
    }

    @Transactional
    public void generateInvoicesForPeriod(int month, int year) {
        // 1. Récupérer toutes les agences actives
        List<TenantClient.AgencyDto> activeAgencies;
        try {
            activeAgencies = tenantClient.getActiveAgenciesInternal();
        } catch (Exception e) {
            log.error("Impossible de récupérer la liste des agences actives : {}", e.getMessage());
            return;
        }

        // 2. Générer les factures
        for (TenantClient.AgencyDto agency : activeAgencies) {
            // Ne pas facturer les agences en gratuit/sans prix ou suspendues
            if (agency.getSubscriptionPriceMonthly() == null || agency.getSubscriptionPriceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Générer si pas déjà existante pour ce mois/année
            SaaSInvoice invoice = SaaSInvoice.builder()
                .agencyId(agency.getId())
                .planName(agency.getSubscriptionPlanName())
                .amount(agency.getSubscriptionPriceMonthly())
                .periodMonth(month)
                .periodYear(year)
                .dueDate(LocalDate.now().plusDays(10)) // Échéance à 10 jours
                .status(SaaSInvoice.InvoiceStatus.UNPAID)
                .build();

            invoiceRepository.save(invoice);
            log.info("Facture SaaS générée pour l'agence {} ({} XOF)", agency.getName(), agency.getSubscriptionPriceMonthly());
        }
    }

    /**
     * Tâche planifiée : Vérifie chaque jour les factures impayées dépassées de leur échéance
     * Suspend les agences correspondantes
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void checkForOverdueInvoices() {
        log.info("Vérification des factures SaaS en retard de paiement...");
        LocalDate today = LocalDate.now();
        
        List<SaaSInvoice> unpaidInvoices = invoiceRepository.findAll().stream()
            .filter(inv -> inv.getStatus() == SaaSInvoice.InvoiceStatus.UNPAID && inv.getDueDate().isBefore(today))
            .toList();

        for (SaaSInvoice invoice : unpaidInvoices) {
            invoice.setStatus(SaaSInvoice.InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);

            // Suspendre l'accès de l'agence via Feign
            try {
                tenantClient.suspendAgency(invoice.getAgencyId(), "Défaut de paiement de la facture SaaS de " + invoice.getPeriodMonth() + "/" + invoice.getPeriodYear());
                log.warn("Agence {} suspendue pour facture impayée (ID facture: {})", invoice.getAgencyId(), invoice.getId());
            } catch (Exception e) {
                log.error("Erreur lors de la suspension de l'agence {} via Feign: {}", invoice.getAgencyId(), e.getMessage());
            }
        }
    }

    public Page<SaaSInvoice> getAgencyInvoices(UUID agencyId, Pageable pageable) {
        return invoiceRepository.findByAgencyId(agencyId, pageable);
    }

    public Page<SaaSInvoice> getAllInvoices(SaaSInvoice.InvoiceStatus status, Pageable pageable) {
        if (status != null) {
            return invoiceRepository.findByStatus(status, pageable);
        }
        return invoiceRepository.findAll(pageable);
    }

    @Transactional
    public SaaSInvoice payInvoice(UUID invoiceId, String method) {
        SaaSInvoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));

        if (invoice.getStatus() == SaaSInvoice.InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette facture est déjà payée");
        }

        invoice.setStatus(SaaSInvoice.InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setPaymentMethod(method);
        invoiceRepository.save(invoice);

        // Optionnel : réactiver l'agence s'il n'y a plus aucune autre facture en souffrance
        List<SaaSInvoice> remainingOverdues = invoiceRepository.findByAgencyIdAndStatus(invoice.getAgencyId(), SaaSInvoice.InvoiceStatus.OVERDUE);
        if (remainingOverdues.isEmpty()) {
            log.info("L'agence {} a régularisé toutes ses factures.", invoice.getAgencyId());
            // On pourrait appeler tenantClient pour réactiver l'agence ici
        }

        return invoice;
    }
}
