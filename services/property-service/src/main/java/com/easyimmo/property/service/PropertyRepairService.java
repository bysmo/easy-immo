package com.easyimmo.property.service;

import com.easyimmo.property.dto.RepairRequest;
import com.easyimmo.property.model.Property;
import com.easyimmo.property.model.PropertyRepair;
import com.easyimmo.property.repository.PropertyRepairRepository;
import com.easyimmo.property.repository.PropertyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyRepairService {

    private final PropertyRepairRepository repairRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public PropertyRepair createRepair(UUID agencyId, UUID propertyId, RepairRequest request, UUID agentId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        PropertyRepair repair = PropertyRepair.builder()
            .property(property)
            .agencyId(agencyId)
            .description(request.getDescription())
            .estimatedCost(request.getEstimatedCost())
            .actualCost(request.getActualCost())
            .repairDate(request.getRepairDate())
            .status(request.getStatus())
            .payer(request.getPayer())
            .notes(request.getNotes())
            .createdBy(agentId)
            .build();

        // Si la réparation commence ou est planifiée, on peut éventuellement passer le bien en maintenance
        if (repair.getStatus() == PropertyRepair.RepairStatus.IN_PROGRESS && property.getStatus() == Property.PropertyStatus.AVAILABLE) {
            property.setStatus(Property.PropertyStatus.MAINTENANCE);
            propertyRepository.save(property);
        }

        return repairRepository.save(repair);
    }

    public Page<PropertyRepair> getRepairsByProperty(UUID agencyId, UUID propertyId, Pageable pageable) {
        // Validation que le bien appartient à l'agence
        propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        return repairRepository.findByPropertyId(propertyId, pageable);
    }

    public Page<PropertyRepair> getRepairsByAgency(UUID agencyId, Pageable pageable) {
        return repairRepository.findByAgencyId(agencyId, pageable);
    }

    @Transactional
    public PropertyRepair updateRepair(UUID agencyId, UUID repairId, RepairRequest request) {
        PropertyRepair repair = repairRepository.findById(repairId)
            .filter(r -> r.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réparation introuvable"));

        repair.setDescription(request.getDescription());
        repair.setEstimatedCost(request.getEstimatedCost());
        repair.setActualCost(request.getActualCost());
        repair.setRepairDate(request.getRepairDate());
        
        PropertyRepair.RepairStatus oldStatus = repair.getStatus();
        repair.setStatus(request.getStatus());
        repair.setPayer(request.getPayer());
        repair.setNotes(request.getNotes());

        // Gérer le statut du bien en fonction de l'évolution du statut de la réparation
        Property property = repair.getProperty();
        if (repair.getStatus() == PropertyRepair.RepairStatus.DONE && oldStatus != PropertyRepair.RepairStatus.DONE) {
            // Si c'est terminé et que le bien était en maintenance, on le remet disponible
            if (property.getStatus() == Property.PropertyStatus.MAINTENANCE) {
                property.setStatus(Property.PropertyStatus.AVAILABLE);
                propertyRepository.save(property);
            }
        } else if (repair.getStatus() == PropertyRepair.RepairStatus.IN_PROGRESS && oldStatus == PropertyRepair.RepairStatus.PENDING) {
            if (property.getStatus() == Property.PropertyStatus.AVAILABLE) {
                property.setStatus(Property.PropertyStatus.MAINTENANCE);
                propertyRepository.save(property);
            }
        }

        return repairRepository.save(repair);
    }

    @Transactional
    public void deleteRepair(UUID agencyId, UUID repairId) {
        PropertyRepair repair = repairRepository.findById(repairId)
            .filter(r -> r.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réparation introuvable"));

        // Remettre disponible si on supprime la seule réparation en cours qui bloquait le bien
        Property property = repair.getProperty();
        if (property.getStatus() == Property.PropertyStatus.MAINTENANCE && repair.getStatus() == PropertyRepair.RepairStatus.IN_PROGRESS) {
            property.setStatus(Property.PropertyStatus.AVAILABLE);
            propertyRepository.save(property);
        }

        repairRepository.delete(repair);
    }
}
