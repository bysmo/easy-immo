package com.easyimmo.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rent_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rent_payment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RentPayment rentPayment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String transactionRef;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
