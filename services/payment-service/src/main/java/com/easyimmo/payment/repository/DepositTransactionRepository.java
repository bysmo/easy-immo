package com.easyimmo.payment.repository;

import com.easyimmo.payment.model.DepositTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, UUID> {
    List<DepositTransaction> findByDepositPaymentIdOrderByPaymentDateDesc(UUID depositPaymentId);
}
