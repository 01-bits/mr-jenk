package com.api.media_service.exception;

/**
 * MediaNotFoundException
 */
public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException(String id) {
        super("Media not found: " + id);
    }
}