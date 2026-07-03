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
@Table(name = "owners", indexes = {
    @Index(name = "idx_owners_agency_id", columnList = "agency_id"),
    @Index(name = "idx_owners_phone", columnList = "phone")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String nationalId;

    @Column(columnDefinition = "TEXT")
    private String address;

    // Informations bancaires pour reversements
    @Column(length = 100)
    private String bankName;

    @Column(length = 100)
    private String bankAccount;

    @Column(length = 20)
    private String mobileMoneyPhone;

    @Column(length = 20)
    private String mobileMoneyProvider; // mtn, orange

    // Pourcentage du loyer reversé au propriétaire (par défaut 80%)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercentage = BigDecimal.valueOf(80.00);

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Property> properties = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
