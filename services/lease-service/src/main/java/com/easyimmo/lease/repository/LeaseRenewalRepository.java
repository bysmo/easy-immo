package com.easyimmo.lease.repository;

import com.easyimmo.lease.model.LeaseRenewal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeaseRenewalRepository extends JpaRepository<LeaseRenewal, UUID> {
    List<LeaseRenewal> findByLeaseIdOrderByCreatedAtDesc(UUID leaseId);
}
