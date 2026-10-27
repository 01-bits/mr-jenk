package com.api.product.exception;

/**
 * ProductNotFoundException
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super("Product not found: " + id);
    }
}