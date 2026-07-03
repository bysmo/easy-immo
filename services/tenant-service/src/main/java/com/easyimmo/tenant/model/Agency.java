package com.easyimmo.tenant.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    @Column(length = 5)
    private String country;  // ISO: BJ, CI, SN, BF, ML, NE, TG, GW

    @Column(length = 20)
    private String phone;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgencyStatus status = AgencyStatus.TRIAL;

    @Column(name = "keycloak_group_id")
    private String keycloakGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlan subscriptionPlan;

    private LocalDateTime trialEndsAt;

    private LocalDateTime suspendedAt;

    @Column(columnDefinition = "TEXT")
    private String suspensionReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AgencyStatus {
        TRIAL, ACTIVE, SUSPENDED, CANCELLED
    }
}
