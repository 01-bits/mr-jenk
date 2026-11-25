package com.buy01.user.config;

import java.util.HashMap;
import java.util.Map;

import javax.naming.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.buy01.user.exception.DuplicateResourceException;
import com.buy01.user.exception.ResourceNotFoundException;

import feign.FeignException;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ ResourceNotFoundException.class })
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(DuplicateResourceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> MethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> BadRequest(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeign(FeignException ex) {
        int status = ex.status();
        // 4xx from media-service (bad file type, too large...): pass its answer to the
        // client
        if (status >= 400 && status < 500) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.contentUTF8());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Media service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Invalid credentials: email or password is incorrect.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, UsernameNotFoundException.class })
    public ResponseEntity<ProblemDetail> LoginInvalidCred(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

}