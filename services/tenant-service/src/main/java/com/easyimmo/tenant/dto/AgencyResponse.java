package com.easyimmo.tenant.dto;

import com.easyimmo.tenant.model.Agency;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AgencyResponse {
    private UUID id;
    private String name;
    private String logoUrl;
    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;
    private Agency.AgencyStatus status;
    private String subscriptionPlanName;
    private BigDecimal subscriptionPriceMonthly;
    private LocalDateTime trialEndsAt;
    private LocalDateTime createdAt;
}
