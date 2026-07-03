package com.easyimmo.lease.repository;

import com.easyimmo.lease.model.Lease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    Page<Lease> findByAgencyId(UUID agencyId, Pageable pageable);

    Page<Lease> findByAgencyIdAndStatus(UUID agencyId, Lease.LeaseStatus status, Pageable pageable);

    List<Lease> findByTenantId(UUID tenantId);

    Optional<Lease> findFirstByTenantIdAndStatus(UUID tenantId, Lease.LeaseStatus status);

    @Query("SELECT l FROM Lease l WHERE l.tenant.keycloakUserId = :keycloakUserId AND l.status = 'ACTIVE'")
    Optional<Lease> findActiveLeaseByKeycloakUserId(String keycloakUserId);

    @Query("SELECT l FROM Lease l WHERE l.tenant.keycloakUserId = :keycloakUserId")
    List<Lease> findAllLeasesByKeycloakUserId(String keycloakUserId);

    @Query("SELECT l FROM Lease l WHERE l.agencyId = :agencyId AND (" +
           "LOWER(l.tenant.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.tenant.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "l.tenant.phone LIKE CONCAT('%', :search, '%'))")
    Page<Lease> searchByAgency(UUID agencyId, String search, Pageable pageable);

    boolean existsByPropertyIdAndStatus(UUID propertyId, Lease.LeaseStatus status);
}
