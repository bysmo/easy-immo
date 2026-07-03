package com.easyimmo.payment.repository;

import com.easyimmo.payment.model.RentPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentPaymentRepository extends JpaRepository<RentPayment, UUID> {

    Page<RentPayment> findByAgencyId(UUID agencyId, Pageable pageable);

    Page<RentPayment> findByAgencyIdAndStatus(UUID agencyId, com.easyimmo.payment.model.PaymentStatus status, Pageable pageable);

    List<RentPayment> findByLeaseIdOrderByPeriodYearDescPeriodMonthDesc(UUID leaseId);

    Optional<RentPayment> findByLeaseIdAndPeriodMonthAndPeriodYear(UUID leaseId, Integer periodMonth, Integer periodYear);

    @Query("SELECT r FROM RentPayment r WHERE r.leaseId IN :leaseIds ORDER BY r.periodYear DESC, r.periodMonth DESC")
    List<RentPayment> findByLeaseIds(List<UUID> leaseIds);

    @Query("SELECT r FROM RentPayment r WHERE r.leaseId IN :leaseIds AND r.status = 'PENDING' OR r.status = 'PARTIAL' OR r.status = 'LATE'")
    List<RentPayment> findUnpaidByLeaseIds(List<UUID> leaseIds);
}
