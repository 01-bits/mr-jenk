package com.api.media_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ProductApi
 */
@FeignClient(name = "product-service")
public interface ProductApi {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OwnerView(String userId) {
    }

    @GetMapping("/products/{id}/owner")
    OwnerView owner(@PathVariable("id") String id);
}