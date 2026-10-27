package com.api.media_service.client;

import org.springframework.stereotype.Component;

import com.api.media_service.exception.ProductNotFoundException;
import com.api.media_service.exception.UpstreamUnavailableException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

/**
 * ProductClient
 */
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final ProductApi api;

    public void assertOwnedBy(String productId, String userId) {
        ProductApi.OwnerView owner;
        try {
            owner = api.owner(productId);
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (FeignException e) {
            throw new UpstreamUnavailableException("Product service unavailable", e);
        }
        if (owner == null || !userId.equals(owner.userId())) {
            throw new ProductNotFoundException(productId);
        }
    }

}