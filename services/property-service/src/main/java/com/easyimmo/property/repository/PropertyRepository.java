package com.easyimmo.property.repository;

import com.easyimmo.property.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Page<Property> findByAgencyId(UUID agencyId, Pageable pageable);

    Page<Property> findByAgencyIdAndStatus(UUID agencyId, Property.PropertyStatus status, Pageable pageable);

    // Recherche publique (pour l'app mobile - tous les biens AVAILABLE)
    @Query("SELECT p FROM Property p WHERE p.status = 'AVAILABLE' AND (" +
           "LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Property> findAvailableProperties(String search, Pageable pageable);

    @Query("SELECT p FROM Property p WHERE p.status = 'AVAILABLE' AND " +
           "p.currentRent >= :minRent AND p.currentRent <= :maxRent AND " +
           "(:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:type IS NULL OR p.type = :type)")
    Page<Property> searchAvailable(
        String city,
        java.math.BigDecimal minRent,
        java.math.BigDecimal maxRent,
        Property.PropertyType type,
        Pageable pageable
    );

    @Query("SELECT p FROM Property p WHERE p.agencyId = :agencyId AND (" +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.reference) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Property> searchByAgency(UUID agencyId, String search, Pageable pageable);

    long countByAgencyIdAndStatus(UUID agencyId, Property.PropertyStatus status);

    long countByAgencyId(UUID agencyId);

    List<Property> findByAgencyIdAndStatus(UUID agencyId, Property.PropertyStatus status);
}
