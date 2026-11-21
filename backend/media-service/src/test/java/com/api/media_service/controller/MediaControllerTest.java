package com.api.media_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

import com.api.media_service.config.SecurityConfig;
import com.api.media_service.dto.MediaResponse;
import com.api.media_service.exception.InvalidMediaException;
import com.api.media_service.exception.MediaNotFoundException;
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

    // =====================================================================
    // GET /media/images/{id}
    // =====================================================================

    @Test
    void get_shouldReturnImageWithCachingHeadersWithoutAuthentication() throws Exception {
        when(service.metadata("media-1")).thenReturn(media);
        when(service.content(media)).thenReturn(new ByteArrayInputStream(IMAGE_BYTES));

        mockMvc.perform(get("/media/images/media-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(IMAGE_BYTES))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "3"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"media-1\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=86400, public"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"));
    }

    @Test
    void get_shouldReturn304AndNotLoadContentWhenEtagMatches() throws Exception {
        when(service.metadata("media-1")).thenReturn(media);

        mockMvc.perform(get("/media/images/media-1").header(HttpHeaders.IF_NONE_MATCH, "\"media-1\""))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));

        // the whole point of the 304: storage is never touched
        verify(service, never()).content(any(Media.class));
    }

    @Test
    void get_shouldReturn200WhenEtagDoesNotMatch() throws Exception {
        when(service.metadata("media-1")).thenReturn(media);
        when(service.content(media)).thenReturn(new ByteArrayInputStream(IMAGE_BYTES));

        mockMvc.perform(get("/media/images/media-1").header(HttpHeaders.IF_NONE_MATCH, "\"other\""))
                .andExpect(status().isOk());
    }

    @Test
    void get_shouldReturn404WhenMediaNotFound() throws Exception {
        when(service.metadata("missing")).thenThrow(new MediaNotFoundException("missing"));

        mockMvc.perform(get("/media/images/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Media not found: missing"));
    }

    @Test
    void get_shouldReturn503WhenStorageFails() throws Exception {
        when(service.metadata("media-1")).thenReturn(media);
        when(service.content(media)).thenThrow(new StorageException("Failed to read object media-1.png", null));

        mockMvc.perform(get("/media/images/media-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Storage unavailable"));
    }

    // =====================================================================
    // GET /media/images?productIds=...
    // =====================================================================

    @Test
    void byProducts_shouldReturnUrlsGroupedByProductWithoutAuthentication() throws Exception {
        when(service.urlsByProduct(List.of("product-1", "product-2"))).thenReturn(Map.of(
                "product-1", List.of("/media/images/media-1"),
                "product-2", List.of()));

        mockMvc.perform(get("/media/images")
                .param("productIds", "product-1")
                .param("productIds", "product-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product-1[0]").value("/media/images/media-1"))
                .andExpect(jsonPath("$.product-2.length()").value(0));
    }

    @Test
    void byProducts_shouldAcceptExactlyOneHundredIds() throws Exception {
        List<String> ids = IntStream.range(0, 100).mapToObj(i -> "p" + i).toList();
        when(service.urlsByProduct(ids)).thenReturn(Map.of());

        mockMvc.perform(get("/media/images").param("productIds", ids.toArray(String[]::new)))
                .andExpect(status().isOk());
    }

    @Test
    void byProducts_shouldReturn400WhenMoreThanOneHundredIds() throws Exception {
        List<String> ids = IntStream.range(0, 101).mapToObj(i -> "p" + 1).toList();
        mockMvc.perform(get("/media/images").param("productIds", ids.toArray(String[]::new)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Too many product ids (max 100)"));

        verifyNoInteractions(service);
    }

    @Test
    void byProducts_shouldReturn400WhenProductIdsMissing() throws Exception {
        mockMvc.perform(get("/media/images"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // =====================================================================
    // DELETE /media/images/{id}
    // =====================================================================

    @Test
    void delete_shouldReturn204AndUseSellerIdFromHeader() throws Exception {
        mockMvc.perform(asSeller(delete("/media/images/media-1")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete("media-1", "seller-1");
    }

    @Test
    void delete_shouldReturn404WhenMediaNotFoundOrNotOwned() throws Exception {
        doThrow(new MediaNotFoundException("media-1")).when(service).delete("media-1", "seller-1");

        mockMvc.perform(asSeller(delete("/media/images/media-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Media not found: media-1"));
    }

    @Test
    void delete_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/media/images/media-1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void delete_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(delete("/media/images/media-1")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // =====================================================================
    // DELETE /media/images?productId=...
    // =====================================================================

    @Test
    void deleteByProduct_shouldReturn204AndUseSellerIdFromHeader() throws Exception {
        mockMvc.perform(asSeller(delete("/media/images").param("productId", "product-1")))
                .andExpect(status().isNoContent());

        verify(service).deleteByProduct("product-1", "seller-1");
    }

    @Test
    void deleteByProduct_shouldReturn400WhenProductIdMissing() throws Exception {
        mockMvc.perform(asSeller(delete("/media/images")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deleteByProduct_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/media/images").param("productId", "product-1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void deleteByProduct_shouldReturn403ForClientRole() throws Exception {
        mockMvc.perform(asClient(delete("/media/images").param("productId", "product-1")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // =====================================================================
    // everything else
    // =====================================================================

    @Test
    void unknownRoute_shouldBeDeniedEvenForSeller() throws Exception {
        mockMvc.perform(asSeller(put("/media/images/media-1")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void unexpectedError_shouldReturn500WithoutLeakingDetails() throws Exception {
        when(service.metadata("media-1")).thenThrow(new IllegalStateException("mongo password is hunter2"));

        mockMvc.perform(get("/media/images/media-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

}