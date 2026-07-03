package com.easyimmo.lease.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TenantResponse {
    private UUID id;
    private String keycloakUserId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String email;
    private String nationalId;
    private String profession;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private boolean hasActiveLease;
    private LocalDateTime createdAt;
}
