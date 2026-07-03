package com.easyimmo.lease.repository;

import com.easyimmo.lease.model.LeaseTermination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseTerminationRepository extends JpaRepository<LeaseTermination, UUID> {
    Optional<LeaseTermination> findByLeaseId(UUID leaseId);
}
