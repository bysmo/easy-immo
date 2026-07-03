package com.easyimmo.lease.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "leases", indexes = {
    @Index(name = "idx_leases_agency_id", columnList = "agency_id"),
    @Index(name = "idx_leases_property_id", columnList = "property_id"),
    @Index(name = "idx_leases_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lease {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId; // ID logique du bien de property-service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tenant tenant;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false)
    private Integer paymentDay = 5; // Jour d'échéance par défaut le 5 de chaque mois

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaseStatus status = LeaseStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String contractPdfUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<LeaseRenewal> renewals = new ArrayList<>();

    @OneToOne(mappedBy = "lease", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LeaseTermination termination;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum LeaseStatus {
        ACTIVE, TERMINATED, EXPIRED
    }
}
