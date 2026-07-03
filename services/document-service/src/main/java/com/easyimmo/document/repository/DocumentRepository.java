package com.easyimmo.document.repository;

import com.easyimmo.document.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByAgencyId(UUID agencyId, Pageable pageable);
    
    Optional<Document> findByTargetIdAndType(UUID targetId, Document.DocumentType type);
}
