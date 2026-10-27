
package com.buy01.user.DTO;

import java.time.Instant;



/**
 * InnerMediaResponse
 */
public record MediaResponse(String id, String url, String productId,
        String contentType, long size, Instant createdAt) {
   
}