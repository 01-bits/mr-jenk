package com.api.product.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * MediaApi
 */

@FeignClient(name = "media-service")
public interface MediaApi {

    @GetMapping("/media/images")
    Map<String, List<String>> imagesByProducts(@RequestParam List<String> productIds);

    @DeleteMapping("/media/images")
    void deleteByProduct(@RequestParam String productId,
            @RequestHeader Map<String, String> headers);
}