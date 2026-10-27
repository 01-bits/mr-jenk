package com.api.media_service.exception;

import java.time.Instant;
import java.util.Map;

/**
 * ApiError
 */
public record ApiError(Instant timestamp, int status, String error, String message,
        Map<String, String> fieldErrors) {
}