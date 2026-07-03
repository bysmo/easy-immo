package com.easyimmo.payment.repository;

import com.easyimmo.payment.model.MobileMoneyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MobileMoneyRequestRepository extends JpaRepository<MobileMoneyRequest, UUID> {
    Optional<MobileMoneyRequest> findByExternalRef(String externalRef);
}
