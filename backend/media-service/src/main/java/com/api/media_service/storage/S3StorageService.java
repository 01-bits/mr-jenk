package com.api.media_service.storage;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.api.media_service.exception.MediaNotFoundException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * S3StorageService
 */
@Service
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final String bucket;

    S3StorageService(S3Client s3, @Value("${media.storage.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @PostConstruct
    void ensureBucket() {
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                try {
                    s3.headBucket(b -> b.bucket(bucket));
                } catch (NoSuchBucketException e) {
                    s3.createBucket(b -> b.bucket(bucket));
                    log.info("Created bucket {}", bucket);
                }
                return;
            } catch (SdkException e) {
                log.warn("Storage not ready (attempt {}/10): {}", attempt, e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Cannot reach object storage / create bucket: " + bucket);
    }

    @Override
    public void save(String key, InputStream data, long size, String contentType) {
        try {
            s3.putObject(b -> b.bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(size)
                    .build(),
                    RequestBody.fromInputStream(data, size));
        } catch (SdkException e) {
            throw new StorageException("Failed to store object " + key, e);
        }
    }

    @Override
    public InputStream load(String key) {
        try {
            return s3.getObject(b -> b.bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            throw new MediaNotFoundException(key);
        } catch (SdkException e) {
            throw new StorageException("Failed to read object " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete object " + key, e);
        }
    }
}