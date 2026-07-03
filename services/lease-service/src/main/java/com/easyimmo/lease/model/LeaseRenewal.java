package com.easyimmo.lease.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lease_renewals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseRenewal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lease lease;

    @Column(nullable = false)
    private LocalDate newEndDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newMonthlyRent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
