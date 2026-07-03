package com.easyimmo.tenant.repository;

import com.easyimmo.tenant.model.AgencySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencySubscriptionRepository extends JpaRepository<AgencySubscription, UUID> {
    Optional<AgencySubscription> findTopByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
}
