package com.api.media_service.storage;

import java.io.InputStream;

/**
 * StorageService
 */
public interface StorageService {
    void save(String key, InputStream data, long size, String contentType);

    InputStream load(String key);

    void delete(String key);
}