package com.easyimmo.property.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket-name}") String bucketName) {

        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();

        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket MinIO créé : {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la vérification/création du bucket MinIO", e);
        }
    }

    /**
     * Upload une photo de bien immobilier
     * @return Le chemin de l'objet dans MinIO (clé)
     */
    public String uploadPropertyPhoto(UUID agencyId, UUID propertyId, MultipartFile file) {
        String objectName = String.format(
            "agencies/%s/properties/%s/photos/%s_%s",
            agencyId, propertyId,
            UUID.randomUUID(),
            sanitizeFilename(file.getOriginalFilename())
        );

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            log.info("Photo uploadée : {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Erreur upload MinIO: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload de la photo", e);
        }
    }

    /**
     * Génère une URL signée temporaire (1 heure) pour accès sécurisé
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(1, TimeUnit.HOURS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur génération URL signée: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Supprime un fichier du bucket
     */
    public void deleteObject(String objectName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            log.info("Objet supprimé de MinIO : {}", objectName);
        } catch (Exception e) {
            log.error("Erreur suppression MinIO: {}", e.getMessage());
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "photo.jpg";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
