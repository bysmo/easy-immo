package com.easyimmo.property.repository;

import com.easyimmo.property.model.PropertyRentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyRentHistoryRepository extends JpaRepository<PropertyRentHistory, UUID> {
    List<PropertyRentHistory> findByPropertyIdOrderByEffectiveDateDesc(UUID propertyId);
}
