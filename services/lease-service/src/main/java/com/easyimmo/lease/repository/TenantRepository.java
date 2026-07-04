package com.easyimmo.lease.repository;

import com.easyimmo.lease.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByPhone(String phone);

    Optional<Tenant> findByKeycloakUserId(String keycloakUserId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndAgencyId(String phone, UUID agencyId);

    // Utilise agency_id directement — fonctionne même pour les locataires sans bail
    Page<Tenant> findByAgencyId(UUID agencyId, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE t.agencyId = :agencyId AND (" +
           "LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "t.phone LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Tenant> searchByAgency(UUID agencyId, String search, Pageable pageable);
}
