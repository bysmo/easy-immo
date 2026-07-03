package com.easyimmo.payment.repository;

import com.easyimmo.payment.model.OwnerDisbursement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OwnerDisbursementRepository extends JpaRepository<OwnerDisbursement, UUID> {
    Page<OwnerDisbursement> findByAgencyId(UUID agencyId, Pageable pageable);
    
    Page<OwnerDisbursement> findByAgencyIdAndStatus(UUID agencyId, OwnerDisbursement.DisbursementStatus status, Pageable pageable);

    List<OwnerDisbursement> findByOwnerIdOrderByPeriodYearDescPeriodMonthDesc(UUID ownerId);
}
