package com.api.product.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import feign.FeignException;

/**
 * MediaClientTest
 */
@ExtendWith(MockitoExtension.class)
public class MediaClientTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    @Mock
    private MediaApi api;

    private MediaClient client;

    @BeforeEach
    void setUp() {
        client = new MediaClient(api, USER_ID_HEADER, ROLE_HEADER);
    }

    // ---------- imageUrls ----------

    @Test
    void imageUrls_shouldReturnEmptyMapAndNotCallApiWhenNoProductIds() {
        Map<String, List<String>> images = client.imageUrls(List.of());
        assertEquals(Map.of(), images);

        verifyNoInteractions(api);
    }

    @Test
    void imageUrls_shouldReturnImagesFromApi() {
        Map<String, List<String>> fromApi = Map.of(
                "product-1", List.of("https://media/img1.jpg"),
                "product-2", List.of());

        when(api.imagesByProducts(List.of("product-1", "product-2"))).thenReturn(fromApi);

        Map<String, List<String>> images = client.imageUrls(List.of("product-1", "product-2"));
        assertEquals(fromApi, images);
        verify(api).imagesByProducts(List.of("product-1", "product-2"));
    }

    @Test
    void imageUrls_shouldReturnNullWhenApiFails() {
        when(api.imagesByProducts(anyList())).thenThrow(mock(FeignException.class));

        Map<String, List<String>> images = client.imageUrls(List.of("product-1"));

        assertNull(images);
    }


    // ---------- deleteImagesOf ----------
 
    @Test
    void deleteImagesOf_shouldCallApiWithProductIdAndAuthHeaders() {
        client.deleteImagesOf("product-1", "seller-1");
 
        verify(api).deleteByProduct("product-1", Map.of(
                USER_ID_HEADER, "seller-1",
                ROLE_HEADER, "SELLER"));
    }
 
    @Test
    void deleteImagesOf_shouldUseConfiguredHeaderNames() {
        client = new MediaClient(api, "X-Custom-Id", "X-Custom-Role");
 
        client.deleteImagesOf("product-1", "seller-1");
 
        verify(api).deleteByProduct("product-1", Map.of(
                "X-Custom-Id", "seller-1",
                "X-Custom-Role", "SELLER"));
    }
 
    @Test
    void deleteImagesOf_shouldNotThrowWhenApiFails() {
        doThrow(mock(FeignException.class)).when(api).deleteByProduct(anyString(), anyMap());
 
        assertDoesNotThrow(() -> client.deleteImagesOf("product-1", "seller-1"));
    }
}