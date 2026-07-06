package com.easyimmo.property.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "properties", indexes = {
    @Index(name = "idx_properties_agency_id", columnList = "agency_id"),
    @Index(name = "idx_properties_status", columnList = "status"),
    @Index(name = "idx_properties_city", columnList = "city")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Owner owner;

    @Column(length = 50)
    private String reference; // Référence interne agence (ex: AG-001)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropertyType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "char")
    private String country;

    @Column
    private Integer rooms;

    @Column
    private Integer bathrooms;

    @Column(name = "area_sqm", precision = 8, scale = 2)
    private BigDecimal areaSqm;

    @Column
    private Integer floor;

    @Column(name = "current_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentRent;

    @Column(name = "deposit_months", nullable = false)
    private Integer depositMonths = 2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String description;

    // JSON stocké comme texte : ["parking", "gardien", "climatisation", "eau_courante"]
    @Column(columnDefinition = "TEXT")
    private String amenities;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<PropertyPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<PropertyRentHistory> rentHistory = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<PropertyRepair> repairs = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Calculer le montant de la caution
    public BigDecimal getDepositAmount() {
        return currentRent.multiply(BigDecimal.valueOf(depositMonths));
    }

    public enum PropertyType {
        APPARTEMENT, VILLA, STUDIO, BOUTIQUE, BUREAU, ENTREPOT, CHAMBRE, DUPLEX
    }

    public enum PropertyStatus {
        AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED
    }
}
