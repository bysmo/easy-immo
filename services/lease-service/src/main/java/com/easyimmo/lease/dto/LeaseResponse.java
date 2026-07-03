package com.easyimmo.lease.dto;

import com.easyimmo.lease.model.Lease;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LeaseResponse {
    private UUID id;
    private UUID agencyId;
    private UUID propertyId;
    
    // Informations à intégrer (venant éventuellement de property-service)
    private String propertyReference;
    private String propertyAddress;
    private String propertyCity;
    
    private UUID tenantId;
    private String tenantFullName;
    private String tenantPhone;
    private String tenantEmail;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
    private Integer paymentDay;
    private Lease.LeaseStatus status;
    private String contractPdfUrl;
    private String notes;
    private LocalDateTime createdAt;
}
