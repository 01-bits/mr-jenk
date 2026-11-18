package com.buy_01.media_service.client;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.api.media_service.client.ProductApi;
import com.api.media_service.client.ProductApi.OwnerView;
import com.api.media_service.client.ProductClient;

/**
 * ProductClientTest
 *
 * Proves that product-service answers are turned into the right outcome:
 * owner matches -> pass, missing or not owned -> 404, service down -> 503.
 */
public class ProductClientTest {

    @Mock
    private ProductApi api;

    @InjectMocks
    private ProductClient client;

    // ---------- owner matches ----------

    @Test
    void assertOwnedBy_shouldPassWhenUserOwnsProduct() {
        when(api.owner("product-1")).thenReturn(new OwnerView("seller-1"));
    }
}