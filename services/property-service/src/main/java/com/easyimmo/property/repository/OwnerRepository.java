package com.easyimmo.property.repository;

import com.easyimmo.property.model.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {

    Page<Owner> findByAgencyId(UUID agencyId, Pageable pageable);

    List<Owner> findByAgencyId(UUID agencyId);

    @Query("SELECT o FROM Owner o WHERE o.agencyId = :agencyId AND (" +
           "LOWER(o.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "o.phone LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Owner> searchByAgency(UUID agencyId, String search, Pageable pageable);

    long countByAgencyId(UUID agencyId);

    boolean existsByPhoneAndAgencyId(String phone, UUID agencyId);
}
