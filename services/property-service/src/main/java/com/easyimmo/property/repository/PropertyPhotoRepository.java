package com.easyimmo.property.repository;

import com.easyimmo.property.model.PropertyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, UUID> {
    List<PropertyPhoto> findByPropertyIdOrderBySortOrderAsc(UUID propertyId);
    
    // Pour s'assurer qu'une seule photo est de couverture
    List<PropertyPhoto> findByPropertyIdAndIsCoverTrue(UUID propertyId);
}
