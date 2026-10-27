package com.api.media_service.exception;

/**
 * ProductNotFoundException
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super("Product not found: " + id);
    }
}