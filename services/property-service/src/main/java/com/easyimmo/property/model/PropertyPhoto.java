package com.easyimmo.property.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Property property;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;         // URL MinIO (chemin objet)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filename;    // Nom du fichier original

    @Column(nullable = false)
    private Boolean isCover = false;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 50)
    private String contentType; // image/jpeg, image/png, etc.

    @CreationTimestamp
    private LocalDateTime createdAt;
}
