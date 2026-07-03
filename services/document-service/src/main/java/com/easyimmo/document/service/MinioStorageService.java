package com.easyimmo.document.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
     * Upload le contenu binaire d'un PDF généré
     */
    public String uploadDocument(String objectName, byte[] content, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, content.length, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("Document PDF sauvegardé sur MinIO : {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Erreur de stockage MinIO: {}", e.getMessage());
            throw new RuntimeException("Erreur de sauvegarde physique du fichier PDF", e);
        }
    }

    /**
     * URL signée temporaire (1 heure) pour accéder au document
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
            log.error("Erreur génération URL signée : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Supprimer un fichier
     */
    public void deleteDocument(String objectName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            log.info("Document PDF supprimé de MinIO : {}", objectName);
        } catch (Exception e) {
            log.error("Erreur suppression MinIO : {}", e.getMessage());
        }
    }
}
