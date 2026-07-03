package com.easyimmo.document.service;

import com.easyimmo.document.model.Document;
import com.easyimmo.document.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfGeneratorService pdfGenerator;
    private final MinioStorageService storageService;

    /**
     * Génère et stocke un contrat de bail PDF
     */
    @Transactional
    public Document generateLeaseContract(UUID agencyId, UUID leaseId, Map<String, Object> data, UUID agentId) {
        log.info("Génération du PDF de contrat de bail pour le bail {}", leaseId);

        // Compiler et générer
        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml("lease-contract", data);

        // Nom du fichier physique
        String objectName = String.format("agencies/%s/leases/%s/contract.pdf", agencyId, leaseId);
        storageService.uploadDocument(objectName, pdfBytes, "application/pdf");

        // Supprimer l'ancienne fiche si existante (écrasement logique)
        documentRepository.findByTargetIdAndType(leaseId, Document.DocumentType.LEASE_CONTRACT)
            .ifPresent(documentRepository::delete);

        // Enregistrer la métadonnée
        Document document = Document.builder()
            .agencyId(agencyId)
            .type(Document.DocumentType.LEASE_CONTRACT)
            .targetId(leaseId)
            .minioObjectName(objectName)
            .filename("Contrat_Bail_" + leaseId.toString().substring(0, 8) + ".pdf")
            .generatedBy(agentId)
            .build();

        return documentRepository.save(document);
    }

    /**
     * Génère et stocke un reçu de paiement (Loyer)
     */
    @Transactional
    public Document generateRentReceipt(UUID agencyId, UUID rentPaymentId, Map<String, Object> data, UUID agentId) {
        log.info("Génération de quittance de loyer pour le loyer {}", rentPaymentId);

        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml("receipt", data);

        String objectName = String.format("agencies/%s/payments/rents/%s/receipt.pdf", agencyId, rentPaymentId);
        storageService.uploadDocument(objectName, pdfBytes, "application/pdf");

        documentRepository.findByTargetIdAndType(rentPaymentId, Document.DocumentType.RENT_RECEIPT)
            .ifPresent(documentRepository::delete);

        Document document = Document.builder()
            .agencyId(agencyId)
            .type(Document.DocumentType.RENT_RECEIPT)
            .targetId(rentPaymentId)
            .minioObjectName(objectName)
            .filename("Quittance_Loyer_" + rentPaymentId.toString().substring(0, 8) + ".pdf")
            .generatedBy(agentId)
            .build();

        return documentRepository.save(document);
    }

    /**
     * Génère et stocke un reçu de paiement de caution
     */
    @Transactional
    public Document generateDepositReceipt(UUID agencyId, UUID depositPaymentId, Map<String, Object> data, UUID agentId) {
        log.info("Génération de reçu de caution pour la caution {}", depositPaymentId);

        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml("receipt", data);

        String objectName = String.format("agencies/%s/payments/deposits/%s/receipt.pdf", agencyId, depositPaymentId);
        storageService.uploadDocument(objectName, pdfBytes, "application/pdf");

        documentRepository.findByTargetIdAndType(depositPaymentId, Document.DocumentType.DEPOSIT_RECEIPT)
            .ifPresent(documentRepository::delete);

        Document document = Document.builder()
            .agencyId(agencyId)
            .type(Document.DocumentType.DEPOSIT_RECEIPT)
            .targetId(depositPaymentId)
            .minioObjectName(objectName)
            .filename("Recu_Caution_" + depositPaymentId.toString().substring(0, 8) + ".pdf")
            .generatedBy(agentId)
            .build();

        return documentRepository.save(document);
    }

    /**
     * Retourne l'URL de téléchargement signée temporaire pour un document
     */
    public String getDownloadUrl(UUID agencyId, UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .filter(d -> d.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable"));

        return storageService.getPresignedUrl(document.getMinioObjectName());
    }

    public String getDownloadUrlByTarget(UUID agencyId, UUID targetId, Document.DocumentType type) {
        Document document = documentRepository.findByTargetIdAndType(targetId, type)
            .filter(d -> d.getAgencyId().equals(agencyId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable pour cette cible"));

        return storageService.getPresignedUrl(document.getMinioObjectName());
    }
}
