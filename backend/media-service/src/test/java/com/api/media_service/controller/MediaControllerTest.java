package com.api.media_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

import com.api.media_service.config.SecurityConfig;
import com.api.media_service.dto.MediaResponse;
import com.api.media_service.exception.InvalidMediaException;
import com.api.media_service.exception.ProductNotFoundException;
import com.api.media_service.exception.UpstreamUnavailableException;
import com.api.media_service.model.Media;
import com.api.media_service.service.MediaService;
import com.api.media_service.storage.StorageException;

/**
 * MediaControllerTest
 */
@WebMvcTest(MediaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "auth.user-id-header=X-User-Id",
        "auth.user-role-header=X-User-Role"
})
public class MediaControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    private static final byte[] IMAGE_BYTES = { 1, 2, 3 };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService service;

    private Media media;
    private MediaResponse response;

    @BeforeEach
    void setUp() {
        media = new Media();
        media.setId("media-1");
        media.setImagePath("media-1.png");
        media.setProductId("product-1");
        media.setOwnerId("seller-1");
        media.setContentType("image/png");
        media.setSize(IMAGE_BYTES.length);

        response = new MediaResponse("media-1", "/media/images/media-1", "product-1",
                "image/png", 3, Instant.now());
    }

    // ---------- helpers ----------

    private static <B extends AbstractMockHttpServletRequestBuilder<?>> B asSeller(B builder) {
        builder.header(USER_ID_HEADER, "seller-1").header(ROLE_HEADER, "SELLER");
        return builder;
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<?>> B asClient(B builder) {
        builder.header(USER_ID_HEADER, "client-1").header(ROLE_HEADER, "CLIENT");
        return builder;
    }

    private static MockMultipartFile filePart(String name) {
        return new MockMultipartFile("files", name, "image/png", IMAGE_BYTES);
    }

    // =====================================================================
    // POST /media/images
    // =====================================================================

    @Test
    void upload_shouldReturn201AndPassFilesProductAndSellerId() throws Exception {
        when(service.uploadAll(anyList(), eq("product-1"), eq("seller-1"))).thenReturn(List.of(response));

        mockMvc.perform(asSeller(multipart("/media/images").file(filePart("a.png"))
                .file(filePart("b.png")).param("productId", "product-1"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("media-1"))
                .andExpect(jsonPath("$[0].url").value("/media/images/media-1"))
                .andExpect(jsonPath("$[0].productId").value("product-1"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MultipartFile>> files = ArgumentCaptor.forClass(List.class);
        verify(service).uploadAll(files.capture(), eq("product-1"), eq("seller-1"));

        assertEquals(List.of("a.png", "b.png"),
                files.getValue().stream().map(MultipartFile::getOriginalFilename).toList());
    }

    @Test
    void upload_shouldPassNullProductIdWhenNotGiven() throws Exception {
        when(service.uploadAll(anyList(), isNull(), eq("seller-1"))).thenReturn(List.of(response));

        mockMvc.perform(asSeller(multipart("/media/images").file(filePart("a.png"))))
                .andExpect(status().isCreated());

        verify(service).uploadAll(anyList(), isNull(), eq("seller-1"));
    }

    @Test
    void upload_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(multipart("/media/images").file(filePart("a.png")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(service);
    }

    @Test
    void upload_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(multipart("/media/images").file(filePart("a.png"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        verifyNoInteractions(service);
    }

    @Test
    void upload_shouldReturn400WhenNoFilesPart() throws Exception {
        mockMvc.perform(asSeller(multipart("/media/images").param("productId", "product-1")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void upload_shouldReturn400WithMessageWhenServiceRejectsFiles() throws Exception {
        when(service.uploadAll(anyList(), any(), eq("seller-1")))
                .thenThrow(new InvalidMediaException("Too many files (max 5)"));

        mockMvc.perform(asSeller(multipart("/media/images").file(filePart("a.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Too many files (max 5)"));
    }

    @Test
    void upload_shouldReturn404WhenProductNotFoundOrNotOwned() throws Exception {
        when(service.uploadAll(anyList(), eq("product-1"), eq("seller-1")))
                .thenThrow(new ProductNotFoundException("product-1"));

        mockMvc.perform(asSeller(multipart("/media/images")
                .file(filePart("a.png"))
                .param("productId", "product-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: product-1"));
    }

    @Test
    void upload_shouldReturn503WhenProductServiceIsDown() throws Exception {
        when(service.uploadAll(anyList(), eq("product-1"), eq("seller-1")))
                .thenThrow(new UpstreamUnavailableException("Product service unavailable", null));

        mockMvc.perform(asSeller(multipart("/media/images")
                .file(filePart("a.png"))
                .param("productId", "product-1")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Product service unavailable"));
    }

    @Test
    void upload_shouldReturn503WithoutDetailsWhenStorageFails() throws Exception {
        when(service.uploadAll(anyList(), any(), eq("seller-1")))
                .thenThrow(new StorageException("Failed to store object abc.png at s3://internal", null));

        mockMvc.perform(asSeller(multipart("/media/images").file(filePart("a.png"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Storage unavailable"));
    }





    

}