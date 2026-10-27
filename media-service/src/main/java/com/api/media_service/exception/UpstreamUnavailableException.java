package com.api.media_service.exception;

/**
 * UpstreamUnavailableException
 */
public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}