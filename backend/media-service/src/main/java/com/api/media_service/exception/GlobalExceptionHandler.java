package com.api.media_service.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.api.media_service.storage.StorageException;

import jakarta.annotation.Nullable;

/**
 * GlobalExceptionHandler
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MediaNotFoundException.class)
    ResponseEntity<ApiError> notFound(MediaNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidMediaException.class)
    ResponseEntity<ApiError> invalidMedia(InvalidMediaException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ApiError> productNotFound(ProductNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    ResponseEntity<ApiError> upstreamDown(UpstreamUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null);
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<ApiError> storageDown(StorageException ex) {
        System.err.println(ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Storage unavailable", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "Bad Request", "Validation failed", errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "Bad Request", "Malformed or invalid request body", null));
    }

    // catches the remaining built-in MVC errors (405, 415, ...) in the same format
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus s = HttpStatus.valueOf(statusCode.value());
        if (ex instanceof MaxUploadSizeExceededException) {
            return ResponseEntity.badRequest().body(
                    new ApiError(Instant.now(), 400, "Bad Request", "File too large (max 2 MB)", null));
        }
        return ResponseEntity.status(statusCode).headers(headers).body(
                new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), s.getReasonPhrase(), null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> fallback(Exception ex) {
        // log ex here; never leak the stack trace to the client
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus s, String msg, Map<String, String> fields) {
        return ResponseEntity.status(s)
                .body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), msg, fields));
    }

}
