package com.easyimmo.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseCreatedEvent {
    private UUID leaseId;
    private UUID agencyId;
    private UUID propertyId;
    private UUID tenantId;
    private String tenantPhone;
    private String tenantFullName;
    private LocalDate startDate;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
}
