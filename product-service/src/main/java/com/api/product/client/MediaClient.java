package com.api.product.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MediaClient {

    private final MediaApi api;
    private final String userIdHeader;
    private final String roleHeader;

    public MediaClient(MediaApi api,
            @Value("${auth.user-id-header:X-User-Id}") String userIdHeader,
            @Value("${auth.user-role-header:X-User-Role}") String roleHeader) {
        this.api = api;
        this.userIdHeader = userIdHeader;
        this.roleHeader = roleHeader;
    }

    /**
     * One request for any number of products. If Media is down, products come back
     * without images.
     */
    public Map<String, List<String>> imageUrls(Collection<String> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        try {
            return api.imagesByProducts(new ArrayList<>(productIds));
        } catch (FeignException e) {
            log.warn("Could not load images: {}", e.getMessage());
            return null;
        }
    }

    /** Best effort: never fails the product deletion. */
    public void deleteImagesOf(String productId, String userId) {
        try {
            api.deleteByProduct(productId, Map.of(userIdHeader, userId, roleHeader, "SELLER"));
        } catch (FeignException e) {
            log.warn("Could not delete images of product {}: {}", productId, e.getMessage());
        }
    }
}