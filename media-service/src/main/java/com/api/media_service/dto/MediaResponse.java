
package com.api.media_service.dto;

import java.time.Instant;

import com.api.media_service.model.Media;

/**
 * InnerMediaResponse
 */
public record MediaResponse(String id, String url, String productId,
        String contentType, long size, Instant createdAt) {
    public static MediaResponse from(Media m) {
        return new MediaResponse(m.getId(), "/media/images/" + m.getId(), m.getProductId(),
                m.getContentType(), m.getSize(), m.getCreatedAt());
    }
}