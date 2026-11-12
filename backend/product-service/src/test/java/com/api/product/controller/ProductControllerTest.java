package com.api.product.controller;

import java.util.List;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.api.product.config.SecurityConfig;
import com.api.product.dto.ProductRequest;
import com.api.product.dto.ProductResponse;
import com.api.product.service.ProductService;

/**
 * ProductControllerTest
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ProductService service;

    private ProductRequest request;
    private ProductResponse response;

    @BeforeEach
    void SetUp() {
        response = new ProductResponse(
                "product-1",
                "Laptop",
                "Gaming laptop",
                1200.0,
                5L,
                "seller-1",
                List.of("https://media/img1.jpg"),
                Instant.now(),
                Instant.now());

        request = new ProductRequest(
                "Laptop",
                "Gaming laptop",
                1200.0,
                5L);

    }

    // ----------- helpers --------------

    private Page<ProductResponse> onePage() {
        return new PageImpl<>(null)
    }

    @Test
    void list_shouldReturnPagedProductsWithoutAuthentication() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(null);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }
}