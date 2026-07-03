package com.easyimmo.property.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_repairs", indexes = {
    @Index(name = "idx_repairs_property_id", columnList = "property_id"),
    @Index(name = "idx_repairs_agency_id", columnList = "agency_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRepair {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Property property;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(precision = 12, scale = 2)
    private BigDecimal actualCost;

    private LocalDate repairDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepairStatus status = RepairStatus.PENDING;

    // Qui paie la réparation
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RepairPayer payer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum RepairStatus {
        PENDING, IN_PROGRESS, DONE, CANCELLED
    }

    public enum RepairPayer {
        AGENCY, OWNER, TENANT
    }
}
