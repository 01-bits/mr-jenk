package com.api.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.api.product.client.MediaClient;
import com.api.product.dto.ProductRequest;
import com.api.product.dto.ProductResponse;
import com.api.product.exception.ProductNotFoundException;
import com.api.product.model.Product;
import com.api.product.repository.ProductRepository;

import jakarta.validation.constraints.AssertTrue;

/**
 * ProductServiceTest
 */
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository repo;

    @Mock
    private MediaClient mediaClient;

    private ProductService productService;

    private Product product;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        productService = new ProductService(repo, mediaClient);

        product = new Product();
        product.setId("product-1");
        product.setUserId("seller-1");
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setPrice(1200.0);
        product.setQuantity(5l);

        request = new ProductRequest(
                "Laptop",
                "Gaming laptop",
                1200.0,
                5l);
    }

    @Test()
    void create_shouldSaveProductWithSellerIdAndTrimmedName() {
        when(repo.save(any(Product.class))).thenReturn(product);

        ProductResponse res = productService.create(request, "seller-1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repo).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals(saved.getUserId(), "seller-1");
        assertEquals(saved.getName(), "Laptop");
        assertEquals(saved.getDescription(), "Gaming laptop");
        assertEquals(saved.getPrice(), 1200.0);
        assertEquals(saved.getQuantity(), 5);

        assertNotNull(res);
    }

    @Test
    void create_shouldTrimProductName() {
        ProductRequest requestWithSpaces = new ProductRequest(
                "  Laptop  ",
                "Gaming laptop",
                1200.0,
                5l);

        when(repo.save(any(Product.class))).thenReturn(product);
        productService.create(requestWithSpaces, "seller-1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repo).save(captor.capture());

        assertEquals(captor.getValue().getName(), "Laptop");
    }

    @Test
    void get_shouldReturnProductWithImages() {
        when(repo.findById("product-1")).thenReturn(Optional.of(product));
        when(mediaClient.imageUrls(List.of("product-1")))
                .thenReturn(Map.of("product-1", List.of("https://media/img1.jpg")));

        ProductResponse res = productService.get("product-1");
        assertNotNull(res);
        assertEquals(List.of("https://media/img1.jpg"), res.imageUrls());

        verify(repo).findById("product-1");
        verify(mediaClient).imageUrls(List.of("product-1"));
    }

    @Test
    void get_shouldThrowWhenProductNotFound() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        assertTrue(assertThrows(ProductNotFoundException.class, () -> productService.get("missing")).getMessage()
                .contains("missing"));

        verify(mediaClient, never()).imageUrls(anyList());
    }

    @Test
    void update_shouldUpdateOwnedProduct() {
        ProductRequest updateRequest = new ProductRequest(
                "Updated laptop",
                "Updated description",
                1500.0,
                10l);

        when(repo.findById("product-1")).thenReturn(Optional.of(product));
        when(mediaClient.imageUrls(List.of("product-1"))).thenReturn(Map.of());
        when(repo.save(any(Product.class))).thenReturn(product);

        ProductResponse res = productService.update(
                "product-1",
                updateRequest,
                "seller-1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repo).save(captor.capture());

        Product saved = captor.getValue();

        assertEquals(saved.getName(), "Updated laptop");
        assertEquals(saved.getDescription(), "Updated description");
        assertEquals(saved.getPrice(), 1500.0);
        assertEquals(saved.getQuantity(), 10);

        assertNotNull(res);
    }

    @Test
    void update_shouldThrowWhenUserDoesNotOwnProduct() {
        when(repo.findById("product-1")).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class,
                () -> productService.update("product-1", request, "another-seller"));

        verify(repo, never()).save(any(Product.class));
    }

    @Test
    void delete_shouldDeleteOwnedProductAndMedia() {
        when(repo.findById("product-1")).thenReturn(Optional.of(product));

        productService.delete("product-1", "seller-1");

        verify(repo).delete(product);
        verify(mediaClient).deleteImagesOf("product-1", "seller-1");
    }

    @Test
    void delete_shouldNotDeleteMediaWhenUserDoesNotOwnProduct() {
        when(repo.findById("product-1")).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class, () -> productService.delete("product-1", "another-seller"));

        verify(repo, never()).delete(any(Product.class));
        verify(mediaClient, never()).deleteImagesOf(anyString(), anyString());
    }

    @Test
    void ownerOf_shouldReturnProductOwnerId() {
        when(repo.findById("product-1")).thenReturn(Optional.of(product));

        String owner = productService.ownerOf("product-1");

        assertEquals(owner, "seller-1");
    }

    @Test
    void ownerOf_shouldThrowWhenProductNotFound() {
        when(repo.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.ownerOf("missing"));
    }

    @Test
    void list_shouldReturnPageWithImageUrls() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(repo.findAll(pageable)).thenReturn(productPage);
        when(mediaClient.imageUrls(List.of("product-1")))
                .thenReturn(Map.of("product-1", List.of("https://media/img1.jpg")));

        Page<ProductResponse> result = productService.list(pageable);

        assertEquals(result.getTotalElements(), 1);
        assertEquals(result.getContent().size(), 1);
        assertEquals(result.getContent().get(0).imageUrls(), List.of("https://media/img1.jpg"));
    }

    @Test
    void listMine_shouldReturnOnlyProductsForUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(repo.findByUserId("seller-1", pageable)).thenReturn(productPage);
        when(mediaClient.imageUrls(List.of("product-1"))).thenReturn(Map.of());

        Page<ProductResponse> result = productService.listMine("seller-1", pageable);

        assertEquals(result.getTotalElements(), 1);
        verify(repo).findByUserId("seller-1", pageable);
    }

    @Test
    void one_shouldReturnNullImagesWhenMediaClientReturnsNull() {
        when(repo.findById("product-1")).thenReturn(Optional.of(product));
        when(mediaClient.imageUrls(List.of("product-1"))).thenReturn(null);

        ProductResponse res = productService.get("product-1");

        assertNotNull(res);
        assertNull(res.imageUrls());
    }

}
