package com.easyimmo.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mobile_money_requests", indexes = {
    @Index(name = "idx_momo_req_external", columnList = "external_ref"),
    @Index(name = "idx_momo_req_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod provider; // MTN_MOMO ou ORANGE_MONEY

    @Column(nullable = false, length = 5)
    private String countryCode; // BJ, CI, SN, etc.

    @Column(nullable = false, length = 20)
    private String phone; // Numéro débité

    @Column(nullable = false, precision = 12, scale = 2)
    private String amount; // Montant de la transaction

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "XOF";

    @Column(length = 100)
    private String externalRef; // Référence externe retournée par l'opérateur MTN/Orange

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String callbackPayload; // Contenu brut de la réponse webhook reçue

    // Type de règlement : DEPOSIT (caution) ou RENT (loyer)
    @Column(nullable = false, length = 20)
    private String paymentType;

    // ID de la fiche de paiement associée (RentPayment ID ou DepositPayment ID)
    @Column(nullable = false)
    private UUID paymentTargetId;

    @CreationTimestamp
    private LocalDateTime initiatedAt;

    @UpdateTimestamp
    private LocalDateTime completedAt;
}
