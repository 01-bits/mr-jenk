package com.api.media_service.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.media_service.client.ProductApi;
import com.api.media_service.client.ProductApi.OwnerView;
import com.api.media_service.exception.ProductNotFoundException;
import com.api.media_service.exception.UpstreamUnavailableException;

import feign.FeignException;

import com.api.media_service.client.ProductClient;

/**
 * ProductClientTest
 *
 * Proves that product-service answers are turned into the right outcome:
 * owner matches -> pass, missing or not owned -> 404, service down -> 503.
 */
@ExtendWith(MockitoExtension.class)
public class ProductClientTest {

    @Mock
    private ProductApi api;

    @InjectMocks
    private ProductClient client;

    // ---------- owner matches ----------

    @Test
    void assertOwnedBy_shouldPassWhenUserOwnsProduct() {
        when(api.owner("product-1")).thenReturn(new OwnerView("seller-1"));

        assertDoesNotThrow(() -> client.assertOwnedBy("product-1", "seller-1"));

        verify(api).owner("product-1");
    }

    // ---------- reported as 404 ----------

    @Test
    void assertOwnedBy_shouldThrowNotFoundWhenProductBelongsToSomeoneElse() {
        when(api.owner("product-1")).thenReturn(new OwnerView("seller-2"));

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> client.assertOwnedBy("product-1", "seller-1"));

        assertEquals("Product not found: product-1", ex.getMessage());
    }

    @Test
    void assertOwnedBy_shouldThrowNotFoundWhenProductServiceReturns404() {
        when(api.owner("missing")).thenThrow(FeignException.NotFound.class);

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> client.assertOwnedBy("missing", "seller-1"));

        assertEquals("Product not found: missing", ex.getMessage());
    }

    @Test
    void assertOwnedBy_shouldThrowNotFoundWhenOwnerResponseIsNull() {
        when(api.owner("product-1")).thenReturn(null);

        assertThrows(ProductNotFoundException.class,
                () -> client.assertOwnedBy("product-1", "seller-1"));
    }

    @Test
    void assertOwnedBy_shouldThrowNotFoundWhenOwnerIdIsNull() {
        when(api.owner("product-1")).thenReturn(new OwnerView(null));

        assertThrows(ProductNotFoundException.class,
                () -> client.assertOwnedBy("product-1", "seller-1"));
    }

    // ---------- reported as 503 ----------

    @Test
    void assertOwnedBy_shouldThrowUpstreamUnavailableWhenProductServiceFails() {
        when(api.owner("product-1")).thenThrow(FeignException.ServiceUnavailable.class);

        UpstreamUnavailableException ex = assertThrows(UpstreamUnavailableException.class,
                () -> client.assertOwnedBy("product-1", "seller-1"));

        assertEquals("Product service unavailable", ex.getMessage());
    }

    @Test
    void assertOwnedBy_shouldThrowUpstreamUnavailableOnAnyOtherFeignError() {
        when(api.owner("product-1")).thenThrow(FeignException.InternalServerError.class);

        assertThrows(UpstreamUnavailableException.class,
                () -> client.assertOwnedBy("product-1", "seller-1"));
    }
}