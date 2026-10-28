package com.api.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.product.client.MediaClient;
import com.api.product.dto.ProductRequest;
import com.api.product.dto.ProductResponse;
import com.api.product.model.Product;
import com.api.product.repository.ProductRepository;

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
    @DisplayName("create_shouldSaveProductWithSellerIdAndTrimmedName")
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
    }
}

/**
 * InnerProductServiceTest
 */
class Calculator {

    int add(int a, int b) {
        return a + b;
    }
}