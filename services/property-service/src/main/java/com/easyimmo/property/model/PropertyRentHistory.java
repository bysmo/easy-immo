package com.easyimmo.property.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_rent_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Property property;

    @Column(precision = 12, scale = 2)
    private BigDecimal oldRent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newRent;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;  // UUID de l'agent Keycloak

    @CreationTimestamp
    private LocalDateTime createdAt;
}
