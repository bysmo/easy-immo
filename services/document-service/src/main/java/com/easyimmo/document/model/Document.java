package com.easyimmo.document.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_docs_agency", columnList = "agency_id"),
    @Index(name = "idx_docs_target", columnList = "target_id"),
    @Index(name = "idx_docs_type", columnList = "type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType type; // LEASE_CONTRACT, RENT_RECEIPT, DEPOSIT_RECEIPT

    @Column(name = "target_id", nullable = false)
    private UUID targetId; // ID logique (Lease ID, RentPayment ID, DepositPayment ID...)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String minioObjectName; // Clé de stockage MinIO

    @Column(nullable = false, length = 150)
    private String filename;

    @Column(name = "generated_by")
    private UUID generatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum DocumentType {
        LEASE_CONTRACT, RENT_RECEIPT, DEPOSIT_RECEIPT
    }
}
