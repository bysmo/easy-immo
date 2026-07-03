package com.easyimmo.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "owner_disbursements", indexes = {
    @Index(name = "idx_disb_agency_id", columnList = "agency_id"),
    @Index(name = "idx_disb_owner_id", columnList = "owner_id"),
    @Index(name = "idx_disb_period", columnList = "period_year, period_month"),
    @Index(name = "idx_disb_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId; // ID logique du bien immobilier

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; // ID logique du propriétaire

    @Column(nullable = false)
    private Integer periodMonth;

    @Column(nullable = false)
    private Integer periodYear;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRentCollected; // Montant total encaissé pour ce bien ce mois-ci

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ownerSharePct; // Ex: 80.00%

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ownerAmount; // totalRentCollected * sharePct

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal agencyCommission; // totalRentCollected * (100 - sharePct)

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal repairsDeducted = BigDecimal.ZERO; // Somme des réparations payées par propriétaire déduites

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netOwnerAmount; // ownerAmount - repairsDeducted

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisbursementStatus status = DisbursementStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod; // Mode de reversement (espèces, MoMo, Orange, virement)

    private LocalDateTime paidAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum DisbursementStatus {
        PENDING, PAID
    }
}
