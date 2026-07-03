package com.easyimmo.property.service;

import com.easyimmo.property.dto.OwnerRequest;
import com.easyimmo.property.dto.OwnerResponse;
import com.easyimmo.property.model.Owner;
import com.easyimmo.property.repository.OwnerRepository;
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
public class OwnerService {

    private final OwnerRepository ownerRepository;

    @Transactional
    public OwnerResponse createOwner(UUID agencyId, OwnerRequest request) {
        Owner owner = Owner.builder()
            .agencyId(agencyId)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .email(request.getEmail())
            .nationalId(request.getNationalId())
            .address(request.getAddress())
            .bankName(request.getBankName())
            .bankAccount(request.getBankAccount())
            .mobileMoneyPhone(request.getMobileMoneyPhone())
            .mobileMoneyProvider(request.getMobileMoneyProvider())
            .sharePercentage(request.getSharePercentage())
            .notes(request.getNotes())
            .build();

        owner = ownerRepository.save(owner);
        log.info("Propriétaire créé : {} {} pour agence {}", owner.getFirstName(), owner.getLastName(), agencyId);
        return toResponse(owner);
    }

    public Page<OwnerResponse> getOwners(UUID agencyId, String search, Pageable pageable) {
        Page<Owner> owners = (search != null && !search.isBlank())
            ? ownerRepository.searchByAgency(agencyId, search, pageable)
            : ownerRepository.findByAgencyId(agencyId, pageable);
        return owners.map(this::toResponse);
    }

    public OwnerResponse getOwner(UUID agencyId, UUID ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
            .filter(o -> o.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriétaire introuvable"));
        return toResponse(owner);
    }

    @Transactional
    public OwnerResponse updateOwner(UUID agencyId, UUID ownerId, OwnerRequest request) {
        Owner owner = ownerRepository.findById(ownerId)
            .filter(o -> o.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriétaire introuvable"));

        owner.setFirstName(request.getFirstName());
        owner.setLastName(request.getLastName());
        owner.setPhone(request.getPhone());
        owner.setEmail(request.getEmail());
        owner.setNationalId(request.getNationalId());
        owner.setAddress(request.getAddress());
        owner.setBankName(request.getBankName());
        owner.setBankAccount(request.getBankAccount());
        owner.setMobileMoneyPhone(request.getMobileMoneyPhone());
        owner.setMobileMoneyProvider(request.getMobileMoneyProvider());
        owner.setSharePercentage(request.getSharePercentage());
        owner.setNotes(request.getNotes());

        return toResponse(ownerRepository.save(owner));
    }

    @Transactional
    public void deleteOwner(UUID agencyId, UUID ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
            .filter(o -> o.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriétaire introuvable"));

        if (!owner.getProperties().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Impossible de supprimer un propriétaire ayant des biens associés");
        }
        ownerRepository.delete(owner);
    }

    private OwnerResponse toResponse(Owner owner) {
        return OwnerResponse.builder()
            .id(owner.getId())
            .agencyId(owner.getAgencyId())
            .firstName(owner.getFirstName())
            .lastName(owner.getLastName())
            .fullName(owner.getFullName())
            .phone(owner.getPhone())
            .email(owner.getEmail())
            .nationalId(owner.getNationalId())
            .address(owner.getAddress())
            .bankName(owner.getBankName())
            .bankAccount(owner.getBankAccount())
            .mobileMoneyPhone(owner.getMobileMoneyPhone())
            .mobileMoneyProvider(owner.getMobileMoneyProvider())
            .sharePercentage(owner.getSharePercentage())
            .notes(owner.getNotes())
            .propertyCount(owner.getProperties() != null ? owner.getProperties().size() : 0)
            .createdAt(owner.getCreatedAt())
            .build();
    }
}
