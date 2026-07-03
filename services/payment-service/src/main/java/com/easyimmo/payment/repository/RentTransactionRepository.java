package com.easyimmo.payment.repository;

import com.easyimmo.payment.model.RentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RentTransactionRepository extends JpaRepository<RentTransaction, UUID> {
    List<RentTransaction> findByRentPaymentIdOrderByPaymentDateDesc(UUID rentPaymentId);
}
