package com.easyimmo.tenant.repository;

import com.easyimmo.tenant.model.Agency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Agency> findByStatus(Agency.AgencyStatus status, Pageable pageable);

    java.util.List<Agency> findByStatusIn(java.util.List<Agency.AgencyStatus> statuses);

    @Query("SELECT a FROM Agency a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.city) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Agency> searchAgencies(String search, Pageable pageable);

    long countByStatus(Agency.AgencyStatus status);
}
