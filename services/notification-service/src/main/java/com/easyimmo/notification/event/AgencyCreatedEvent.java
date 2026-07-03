package com.easyimmo.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyCreatedEvent {
    private UUID agencyId;
    private String agencyName;
    private String agencyEmail;
    private String adminFirstName;
    private String adminLastName;
    private String adminEmail;
    private String adminPhone;
}
