package com.easyimmo.property.repository;

import com.easyimmo.property.model.PropertyRepair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyRepairRepository extends JpaRepository<PropertyRepair, UUID> {
    Page<PropertyRepair> findByPropertyId(UUID propertyId, Pageable pageable);
    Page<PropertyRepair> findByAgencyId(UUID agencyId, Pageable pageable);
    Page<PropertyRepair> findByPropertyIdAndStatus(UUID propertyId, PropertyRepair.RepairStatus status, Pageable pageable);
}
