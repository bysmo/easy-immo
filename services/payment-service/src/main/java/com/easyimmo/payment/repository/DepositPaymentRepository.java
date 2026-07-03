package com.easyimmo.payment.repository;

import com.easyimmo.payment.model.DepositPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepositPaymentRepository extends JpaRepository<DepositPayment, UUID> {
    Page<DepositPayment> findByAgencyId(UUID agencyId, Pageable pageable);
    Optional<DepositPayment> findByLeaseId(UUID leaseId);
}
