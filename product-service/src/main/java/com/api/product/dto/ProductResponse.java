
package com.api.product.dto;

import java.time.Instant;
import java.util.List;

import com.api.product.model.Product;

/**
 * ProductResponse
 */
public record ProductResponse(
        String id, String name, String description, Double price,
        Long quantity, String userId, List<String> imageUrls,
        Instant createdAt, Instant updatedAt) {

    public static ProductResponse from(Product p, List<String> imageUrls) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getQuantity(), p.getUserId(), imageUrls,
                p.getCreatedAt(), p.getUpdatedAt());
    }
}