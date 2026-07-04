package com.easyimmo.tenant.repository;

import com.easyimmo.tenant.model.Collaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollaboratorRepository extends JpaRepository<Collaborator, UUID> {

    List<Collaborator> findByAgencyId(UUID agencyId);

    boolean existsByEmail(String email);

    Optional<Collaborator> findByKeycloakUserId(String keycloakUserId);
}
