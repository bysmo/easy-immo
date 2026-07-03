package com.easyimmo.lease.client;

import com.easyimmo.lease.config.FeignClientInterceptor;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "PROPERTY-SERVICE", configuration = FeignClientInterceptor.class)
public interface PropertyClient {

    @GetMapping("/api/properties/{id}")
    PropertyDto getPropertyById(@PathVariable("id") UUID id);

    // Endpoint pour modifier directement le statut d'un bien lors d'une action sur un bail
    @PutMapping("/api/properties/{id}/status")
    void updatePropertyStatus(@PathVariable("id") UUID id, @RequestParam("status") String status);

    @Data
    class PropertyDto {
        private UUID id;
        private UUID agencyId;
        private String reference;
        private String type;
        private String address;
        private String city;
        private String country;
        private BigDecimal currentRent;
        private Integer depositMonths;
        private String status; // AVAILABLE, OCCUPIED, etc.
    }
}
