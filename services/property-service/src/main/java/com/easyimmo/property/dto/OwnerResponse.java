package com.easyimmo.property.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OwnerResponse {
    private UUID id;
    private UUID agencyId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String email;
    private String nationalId;
    private String address;
    private String bankName;
    private String bankAccount;
    private String mobileMoneyPhone;
    private String mobileMoneyProvider;
    private BigDecimal sharePercentage;
    private String notes;
    private int propertyCount;
    private LocalDateTime createdAt;
}
