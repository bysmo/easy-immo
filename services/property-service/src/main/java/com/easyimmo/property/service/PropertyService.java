package com.easyimmo.property.service;

import com.easyimmo.property.dto.PropertyRequest;
import com.easyimmo.property.dto.PropertyResponse;
import com.easyimmo.property.model.Owner;
import com.easyimmo.property.model.Property;
import com.easyimmo.property.model.PropertyPhoto;
import com.easyimmo.property.model.PropertyRentHistory;
import com.easyimmo.property.repository.OwnerRepository;
import com.easyimmo.property.repository.PropertyPhotoRepository;
import com.easyimmo.property.repository.PropertyRentHistoryRepository;
import com.easyimmo.property.repository.PropertyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final OwnerRepository ownerRepository;
    private final PropertyPhotoRepository photoRepository;
    private final PropertyRentHistoryRepository rentHistoryRepository;
    private final MinioStorageService storageService;

    @Transactional
    public PropertyResponse createProperty(UUID agencyId, PropertyRequest request) {
        Owner owner = null;
        if (request.getOwnerId() != null) {
            owner = ownerRepository.findById(request.getOwnerId())
                .filter(o -> o.getAgencyId().equals(agencyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriétaire introuvable"));
        }

        Property property = Property.builder()
            .agencyId(agencyId)
            .owner(owner)
            .reference(request.getReference())
            .type(request.getType())
            .address(request.getAddress())
            .city(request.getCity())
            .country(request.getCountry())
            .rooms(request.getRooms())
            .bathrooms(request.getBathrooms())
            .areaSqm(request.getAreaSqm())
            .floor(request.getFloor())
            .currentRent(request.getCurrentRent())
            .depositMonths(request.getDepositMonths())
            .status(Property.PropertyStatus.AVAILABLE)
            .description(request.getDescription())
            .amenities(request.getAmenities())
            .build();

        property = propertyRepository.save(property);
        log.info("Bien immobilier créé : {} pour agence {}", property.getId(), agencyId);
        return toResponse(property);
    }

    public Page<PropertyResponse> getProperties(UUID agencyId, String search, Property.PropertyStatus status, Pageable pageable) {
        Page<Property> properties;
        if (search != null && !search.isBlank()) {
            properties = propertyRepository.searchByAgency(agencyId, search, pageable);
        } else if (status != null) {
            properties = propertyRepository.findByAgencyIdAndStatus(agencyId, status, pageable);
        } else {
            properties = propertyRepository.findByAgencyId(agencyId, pageable);
        }
        return properties.map(this::toResponse);
    }

    public PropertyResponse getProperty(UUID agencyId, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));
        return toResponse(property);
    }

    @Transactional
    public PropertyResponse updateProperty(UUID agencyId, UUID propertyId, PropertyRequest request) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        if (request.getOwnerId() != null) {
            Owner owner = ownerRepository.findById(request.getOwnerId())
                .filter(o -> o.getAgencyId().equals(agencyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriétaire introuvable"));
            property.setOwner(owner);
        } else {
            property.setOwner(null);
        }

        property.setReference(request.getReference());
        property.setType(request.getType());
        property.setAddress(request.getAddress());
        property.setCity(request.getCity());
        property.setCountry(request.getCountry());
        property.setRooms(request.getRooms());
        property.setBathrooms(request.getBathrooms());
        property.setAreaSqm(request.getAreaSqm());
        property.setFloor(request.getFloor());
        property.setDescription(request.getDescription());
        property.setAmenities(request.getAmenities());
        
        // Si le loyer change, on garde l'historique
        if (request.getCurrentRent().compareTo(property.getCurrentRent()) != 0) {
            updateRentInternal(property, request.getCurrentRent(), "Mise à jour des informations du bien", null);
        }
        
        property.setDepositMonths(request.getDepositMonths());

        return toResponse(propertyRepository.save(property));
    }

    @Transactional
    public void deleteProperty(UUID agencyId, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        if (property.getStatus() == Property.PropertyStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer un bien actuellement occupé");
        }

        // Supprimer les photos associées de MinIO
        for (PropertyPhoto photo : property.getPhotos()) {
            storageService.deleteObject(photo.getUrl());
        }

        propertyRepository.delete(property);
    }

    @Transactional
    public PropertyResponse uploadPhoto(UUID agencyId, UUID propertyId, MultipartFile file, boolean isCover) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        String objectName = storageService.uploadPropertyPhoto(agencyId, propertyId, file);

        // Si c'est la photo de couverture, on enlève l'ancienne couverture
        if (isCover) {
            List<PropertyPhoto> covers = photoRepository.findByPropertyIdAndIsCoverTrue(propertyId);
            for (PropertyPhoto c : covers) {
                c.setIsCover(false);
                photoRepository.save(c);
            }
        }

        PropertyPhoto photo = PropertyPhoto.builder()
            .property(property)
            .url(objectName)
            .filename(file.getOriginalFilename())
            .isCover(isCover)
            .contentType(file.getContentType())
            .sortOrder(property.getPhotos().size() + 1)
            .build();

        photoRepository.save(photo);
        property.getPhotos().add(photo);

        return toResponse(property);
    }

    @Transactional
    public PropertyResponse deletePhoto(UUID agencyId, UUID propertyId, UUID photoId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        PropertyPhoto photo = photoRepository.findById(photoId)
            .filter(ph -> ph.getProperty().getId().equals(propertyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo introuvable"));

        storageService.deleteObject(photo.getUrl());
        photoRepository.delete(photo);
        property.getPhotos().remove(photo);

        return toResponse(property);
    }

    @Transactional
    public PropertyResponse updateRent(UUID agencyId, UUID propertyId, BigDecimal newRent, String reason, UUID agentId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> p.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));

        updateRentInternal(property, newRent, reason, agentId);
        return toResponse(propertyRepository.save(property));
    }

    private void updateRentInternal(Property property, BigDecimal newRent, String reason, UUID agentId) {
        PropertyRentHistory history = PropertyRentHistory.builder()
            .property(property)
            .oldRent(property.getCurrentRent())
            .newRent(newRent)
            .effectiveDate(LocalDate.now())
            .reason(reason)
            .createdBy(agentId)
            .build();

        rentHistoryRepository.save(history);
        property.setCurrentRent(newRent);
    }

    @Transactional
    public void updatePropertyStatus(UUID propertyId, Property.PropertyStatus newStatus) {
        Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bien immobilier introuvable"));
        property.setStatus(newStatus);
        propertyRepository.save(property);
        log.info("Statut du bien {} mis à jour à : {}", propertyId, newStatus);
    }

    public Page<PropertyResponse> getAvailablePropertiesPublic(
            String city, BigDecimal minRent, BigDecimal maxRent, Property.PropertyType type, String search, Pageable pageable) {
        
        Page<Property> properties;
        if (search != null && !search.isBlank()) {
            properties = propertyRepository.findAvailableProperties(search, pageable);
        } else {
            BigDecimal min = minRent != null ? minRent : BigDecimal.ZERO;
            BigDecimal max = maxRent != null ? maxRent : BigDecimal.valueOf(999999999);
            properties = propertyRepository.searchAvailable(city, min, max, type, pageable);
        }
        return properties.map(this::toResponse);
    }

    private PropertyResponse toResponse(Property property) {
        List<PropertyResponse.PhotoDto> photoDtos = property.getPhotos().stream()
            .map(photo -> PropertyResponse.PhotoDto.builder()
                .id(photo.getId())
                .url(storageService.getPresignedUrl(photo.getUrl()))
                .isCover(photo.getIsCover())
                .sortOrder(photo.getSortOrder())
                .build())
            .collect(Collectors.toList());

        String coverPhoto = photoDtos.stream()
            .filter(PropertyResponse.PhotoDto::getIsCover)
            .map(PropertyResponse.PhotoDto::getUrl)
            .findFirst()
            .orElse(photoDtos.isEmpty() ? null : photoDtos.get(0).getUrl());

        return PropertyResponse.builder()
            .id(property.getId())
            .agencyId(property.getAgencyId())
            .reference(property.getReference())
            .type(property.getType())
            .address(property.getAddress())
            .city(property.getCity())
            .country(property.getCountry())
            .rooms(property.getRooms())
            .bathrooms(property.getBathrooms())
            .areaSqm(property.getAreaSqm())
            .floor(property.getFloor())
            .currentRent(property.getCurrentRent())
            .depositMonths(property.getDepositMonths())
            .depositAmount(property.getDepositAmount())
            .status(property.getStatus())
            .description(property.getDescription())
            .amenities(property.getAmenities())
            .ownerId(property.getOwner() != null ? property.getOwner().getId() : null)
            .ownerFullName(property.getOwner() != null ? property.getOwner().getFullName() : null)
            .ownerSharePercentage(property.getOwner() != null ? property.getOwner().getSharePercentage() : null)
            .photos(photoDtos)
            .coverPhotoUrl(coverPhoto)
            .createdAt(property.getCreatedAt())
            .build();
    }
}
