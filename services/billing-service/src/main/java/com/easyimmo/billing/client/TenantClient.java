package com.easyimmo.billing.client;

import com.easyimmo.billing.config.FeignClientInterceptor;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "TENANT-SERVICE", configuration = FeignClientInterceptor.class)
public interface TenantClient {

    @GetMapping("/api/tenants/agencies/{id}")
    AgencyDto getAgencyById(@PathVariable("id") UUID id);

    // Endpoint pour suspendre directement une agence si facture impayée
    @PostMapping("/api/tenants/agencies/{id}/suspend")
    void suspendAgency(@PathVariable("id") UUID id, @RequestParam("reason") String reason);

    // Endpoint d'extraction interne de la liste des agences actives pour facturation batch
    @GetMapping("/api/tenants/agencies/internal/active")
    List<AgencyDto> getActiveAgenciesInternal();

    @Data
    class AgencyDto {
        private UUID id;
        private String name;
        private String email;
        private String status; // TRIAL, ACTIVE, SUSPENDED
        private String subscriptionPlanName;
        private BigDecimal subscriptionPriceMonthly;
    }
}
