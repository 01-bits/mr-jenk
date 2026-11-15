package com.api.product.controller;

import java.util.List;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.api.product.config.SecurityConfig;
import com.api.product.dto.ProductRequest;
import com.api.product.dto.ProductResponse;
import com.api.product.exception.ProductNotFoundException;
import com.api.product.model.Role;
import com.api.product.service.ProductService;

/**
 * ProductControllerTest
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "auth.user-id-header=X-User-Id",
        "auth.user-role-header=X-User-Role"
})
public class ProductControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    private static final String VALID_BODY = """
                {
                    "name": "Laptop",
                    "description": "Gaming laptop",
                    "price": 1200.0,
                    "quantity": 5
                }
            """;

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

    // ---------- helpers ----------------

    private Page<ProductResponse> onePage() {
        return new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
    }

    private static MockHttpServletRequestBuilder asSeller(MockHttpServletRequestBuilder builder) {
        return builder.header(USER_ID_HEADER, "seller-1").header(ROLE_HEADER, Role.SELLER.name());
    }

    private static MockHttpServletRequestBuilder asClient(MockHttpServletRequestBuilder builder) {
        return builder.header(USER_ID_HEADER, "client-1").header(ROLE_HEADER, Role.CLIENT.name());
    }

    // ---------- GET /products ----------

    @Test
    void list_shouldReturnPagedProductsWithoutAuthentication() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(onePage());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value("product-1"))
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.content[0].imageUrls[0]").value("https://media/img1.jpg"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void list_shouldUseDefaultPagingWhenNoParamsGiven() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(onePage());

        mockMvc.perform(get("/products")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void list_shouldPassRequestedPageAndCapSizeAtMax() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(onePage());

        mockMvc.perform(get("/products").param("page", "2").param("size", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(captor.capture());

        assertEquals(2, captor.getValue().getPageNumber());
        // spring.data.web.pageable.max-page-size in application.yaml

        assertEquals(100, captor.getValue().getPageSize());
    }

    // ---------- GET /products/me ----------

    @Test
    void mine_shouldReturnProductsOfAuthenticatedSeller() throws Exception {
        when(service.listMine(eq("seller-1"), any(Pageable.class))).thenReturn(onePage());

        mockMvc.perform(asSeller(get("/products/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("seller-1"));

        verify(service).listMine(eq("seller-1"), any(Pageable.class));
    }

    @Test
    void mine_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/products/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(service);
    }

    @Test
    void mine_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(get("/products/me")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verifyNoInteractions(service);
    }

    // ---------- GET /products/{id} ----------

    @Test
    void get_shouldReturnProductWithoutAuthentication() throws Exception {
        when(service.get("product-1")).thenReturn(response);

        mockMvc.perform(get("/products/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product-1"))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.description").value("Gaming laptop"))
                .andExpect(jsonPath("$.price").value(1200.0))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.userId").value("seller-1"))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://media/img1.jpg"));
    }

    @Test
    void get_shouldReturn404WhenProductNotFound() throws Exception {
        when(service.get("missing")).thenThrow(new ProductNotFoundException("missing"));

        mockMvc.perform(get("/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found: missing"));
    }

    // ---------- GET /products/{id}/owner ----------

    @Test
    void owner_shouldReturnOwnerId() throws Exception {
        when(service.ownerOf("product-1")).thenReturn("seller-1");

        mockMvc.perform(get("/products/product-1/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("seller-1"));
    }

    @Test
    void owner_shouldReturn404WhenProductNotFound() throws Exception {
        when(service.ownerOf("missing")).thenThrow(new ProductNotFoundException("missing"));

        mockMvc.perform(get("/products/missing/owner"))
                .andExpect(status().isNotFound());
    }

    // ---------- POST /products ----------

    @Test
    void create_shouldReturn201AndUseSellerIdFromHeader() throws Exception {
        when(service.create(request, "seller-1")).thenReturn(response);

        mockMvc.perform(asSeller(post("/products"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("product-1"))
                .andExpect(jsonPath("$.userId").value("seller-1"));

        verify(service).create(request, "seller-1");
    }

    @Test
    void create_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void create_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(post("/products"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void create_shouldReturn400WithFieldErrorsWhenBodyInvalid() throws Exception {
        String invalidBody = """
                {
                  "name": "   ",
                  "description": "Gaming laptop",
                  "price": -1,
                  "quantity": -5
                }
                """;

        mockMvc.perform(asSeller(post("/products"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        verify(service, never()).create(any(ProductRequest.class), anyString());
    }

    @Test
    void create_shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(asSeller(post("/products"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Laptop\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        verify(service, never()).create(any(ProductRequest.class), anyString());
    }

    @Test
    void create_shouldReturn400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(asSeller(post("/products"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid request body"));

        verify(service, never()).create(any(ProductRequest.class), anyString());
    }

    // ---------- PUT /products/{id} ----------

    @Test
    void update_shouldReturn200AndPassIdBodyAndSellerId() throws Exception {
        when(service.update("product-1", request, "seller-1")).thenReturn(response);

        mockMvc.perform(asSeller(put("/products/product-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product-1"));

        verify(service).update("product-1", request, "seller-1");
    }

    @Test
    void update_shouldReturn404WhenProductNotFoundOrNotOwned() throws Exception {
        when(service.update("product-1", request, "seller-1"))
                .thenThrow(new ProductNotFoundException("product-1"));

        mockMvc.perform(asSeller(put("/products/product-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: product-1"));
    }

    @Test
    void update_shouldReturn400WhenBodyInvalid() throws Exception {
        mockMvc.perform(asSeller(put("/products/product-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\", \"price\": 10, \"quantity\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verify(service, never()).update(anyString(), any(ProductRequest.class), anyString());
    }

    @Test
    void update_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/products/product-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void update_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(put("/products/product-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void delete_shouldReturn204WithEmptyBody() throws Exception {
        mockMvc.perform(asSeller(delete("/products/product-1")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete("product-1", "seller-1");
    }

    @Test
    void delete_shouldReturn404WhenProductNotFoundOrNotOwned() throws Exception {
        doThrow(new ProductNotFoundException("product-1")).when(service).delete("product-1", "seller-1");

        mockMvc.perform(asSeller(delete("/products/product-1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/products/product-1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void delete_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(delete("/products/product-1")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // else

    @Test
    void unknownRoute_shouldBeDeniedEvenForSeller() throws Exception {
        mockMvc.perform(asSeller(patch("/products/product-1")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void unexpectedError_shouldReturn500WithoutLeakingDetails() throws Exception {
        when(service.get("product-1")).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/products/product-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

}