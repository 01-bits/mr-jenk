package com.api.media_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.api.media_service.client.ProductClient;
import com.api.media_service.dto.MediaResponse;
import com.api.media_service.exception.InvalidMediaException;
import com.api.media_service.exception.MediaNotFoundException;
import com.api.media_service.exception.ProductNotFoundException;
import com.api.media_service.model.Media;
import com.api.media_service.repository.MediaRepository;
import com.api.media_service.storage.StorageException;
import com.api.media_service.storage.StorageService;

/**
 * MediaServiceTest
 */
@ExtendWith(MockitoExtension.class)
public class MediaServiceTest {

    private static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final String UUID_PNG_KEY = "[0-9a-f-]{36}\\.png";

    @Mock
    private MediaRepository repo;

    @Mock
    private StorageService storage;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private MediaService service;

    // ---------- helpers ----------

    private static byte[] pngBytes(int size) {
        byte[] b = new byte[size];
        System.arraycopy(PNG_SIGNATURE, 0, b, 0, PNG_SIGNATURE.length);
        return b;
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", pngBytes(64));
    }

    private static MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("files", "file.bin", contentType, content);
    }

    private static Answer<Media> withId(String id) {
        return inv -> {
            Media m = inv.getArgument(0);
            m.setId(id);
            return m;
        };
    }

    private List<String> savedKeys(int expectedCalls) {
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(storage, times(expectedCalls)).save(keys.capture(), any(InputStream.class), anyLong(), anyString());
        return keys.getAllValues();
    }

    private static Media media(String id, String productId, String ownerId) {
        Media m = new Media();
        m.setId(id);
        m.setImagePath(id + ".png");
        m.setProductId(productId);
        m.setOwnerId(ownerId);
        m.setContentType("image/png");
        m.setSize(64);
        return m;
    }

    // =====================================================================
    // uploadAll: request-level validation
    // =====================================================================

    @Test
    void uploadAll_shouldRejectNullFileList() {
        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(null, null, "seller-1"));
        assertEquals("At least one file is required", ex.getMessage());
        verifyNoInteractions(repo, storage, productClient);
    }

    @Test
    void uploadAll_shouldRejectEmptyFileList() {
        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(List.of(), null, "seller-1"));

        assertEquals("At least one file is required", ex.getMessage());
        verifyNoInteractions(repo, storage, productClient);
    }

    @Test
    void uploadAll_shouldRejectMoreThanFiveFiles() {
        List<MultipartFile> six = Collections.nCopies(6, png("a.png"));

        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(six, null, "seller-1"));

        assertEquals("Too many files (max 5)", ex.getMessage());
        verifyNoInteractions(repo, storage, productClient);
    }

    // =====================================================================
    // uploadAll: per-file validation
    // =====================================================================

    @Test
    void uploadAll_shouldRejectEmptyFile() {
        MultipartFile empty = file("image/png", new byte[0]);

        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(List.of(empty), null, "seller-1"));

        assertEquals("File is required", ex.getMessage());
    }

    @Test
    void uploadAll_shouldRejectFileLargerThanTwoMegabytes() {
        MultipartFile tooBig = file("image/png", pngBytes((int) MAX_SIZE + 1));

        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(List.of(tooBig), null, "seller-1"));

        assertEquals("File too large (max 2 MB)", ex.getMessage());
        verifyNoInteractions(storage);
    }

    @Test
    void uploadAll_shouldAcceptFileOfExactlyTwoMegabytes() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));
        MultipartFile maxSize = file("image/png", pngBytes((int) MAX_SIZE));

        assertDoesNotThrow(() -> service.uploadAll(List.of(maxSize), null, "seller-1"));
    }

    @Test
    void uploadAll_shouldRejectNonImageContentType() {
        MultipartFile pdf = file("application/pdf", pngBytes(64));

        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(List.of(pdf), null, "seller-1"));

        assertEquals("Only image files are allowed", ex.getMessage());
    }

    @Test
    void uploadAll_shouldRejectMissingContentType() {
        MultipartFile noType = file(null, pngBytes(64));

        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(List.of(noType), null, "seller-1"));

        assertEquals("Only image files are allowed", ex.getMessage());
    }

    @Test
    void uploadAll_shouldAcceptContentTypeInAnyCase() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));
        MultipartFile upper = file("IMAGE/PNG", pngBytes(64));

        assertDoesNotThrow(() -> service.uploadAll(List.of(upper), null, "seller-1"));
    }

    @Test
    void uploadAll_shouldRejectFileThatClaimsImageButIsNot() {
        MultipartFile fake = file("image/png", "not really a picture".getBytes());

        InvalidMediaException ex = assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(List.of(fake), null, "seller-1"));

        assertEquals("Unsupported image content (allowed: jpeg, png, gif, webp)", ex.getMessage());
        verifyNoInteractions(storage);
    }

    @Test
    void uploadAll_shouldThrowStorageExceptionWhenFileCannotBeRead() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getSize()).thenReturn(64L);
        when(broken.getContentType()).thenReturn("image/png");
        when(broken.getInputStream()).thenThrow(new IOException("disk error"));

        StorageException ex = assertThrows(StorageException.class,
                () -> service.uploadAll(List.of(broken), null, "seller-1"));

        assertEquals("Cannot read the uploaded file", ex.getMessage());
    }

    @Test
    void uploadAll_shouldValidateEveryFileBeforeStoringAny() {
        List<MultipartFile> files = List.of(png("a.png"), png("b.png"), file("text/plain", pngBytes(64)));

        assertThrows(InvalidMediaException.class,
                () -> service.uploadAll(files, "product-1", "seller-1"));

        verifyNoInteractions(repo, storage, productClient);
    }

    // =====================================================================
    // uploadAll: product ownership
    // =====================================================================

    @Test
    void uploadAll_shouldCheckOwnershipOnceWithTrimmedProductId() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1")).thenAnswer(withId("media-2"));

        service.uploadAll(List.of(png("a.png"), png("b.png")), "  product-1  ", "seller-1");

        verify(productClient).assertOwnedBy("product-1", "seller-1");
    }

    @Test
    void uploadAll_shouldSkipOwnershipCheckWhenProductIdIsBlank() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));

        List<MediaResponse> result = service.uploadAll(List.of(png("a.png")), "   ", "seller-1");

        verifyNoInteractions(productClient);
        assertNull(result.get(0).productId());
    }

    @Test
    void uploadAll_shouldStoreNothingWhenProductIsNotOwned() {
        doThrow(new ProductNotFoundException("product-1"))
                .when(productClient)
                .assertOwnedBy("product-1", "seller-1");

        assertThrows(ProductNotFoundException.class,
                () -> service.uploadAll(List.of(png("a.png")), "product-1", "seller-1"));

        verifyNoInteractions(repo, storage);
    }

    // =====================================================================
    // uploadAll: storing
    // =====================================================================

    @Test
    void uploadAll_shouldStoreFileAndReturnResponse() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));

        List<MediaResponse> result = service.uploadAll(List.of(png("a.png")), "product-1", "seller-1");

        assertEquals(1, result.size());
        MediaResponse r = result.get(0);
        assertEquals("media-1", r.id());
        assertEquals("/media/images/media-1", r.url());
        assertEquals("product-1", r.productId());
        assertEquals("image/png", r.contentType());
        assertEquals(64, r.size());

        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(repo).save(saved.capture());
        assertEquals("seller-1", saved.getValue().getOwnerId());
        assertEquals(savedKeys(1).get(0), saved.getValue().getImagePath());
    }

    @Test
    void uploadAll_shouldNameStoredFileWithRandomKeyNeverClientFilename() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));

        service.uploadAll(List.of(png("../../etc/passwd.png")), null, "seller-1");

        String key = savedKeys(1).get(0);
        assertTrue(key.matches(UUID_PNG_KEY), "unexpected key: " + key);
        assertFalse(key.contains("passwd"));
    }

    @Test
    void uploadAll_shouldUseDetectedTypeNotDeclaredType() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));
        // declared jpeg, real content png
        MultipartFile mislabelled = file("image/jpeg", pngBytes(64));

        service.uploadAll(List.of(mislabelled), null, "seller-1");

        verify(storage).save(anyString(), any(InputStream.class), eq(64L), eq("image/png"));
    }

    @Test
    void uploadAll_shouldGiveEachFileItsOwnKey() {
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1")).thenAnswer(withId("media-2"));

        service.uploadAll(List.of(png("a.png"), png("b.png")), null, "seller-1");

        List<String> keys = savedKeys(2);
        assertFalse(keys.get(0).equals(keys.get(1)));
    }

    // =====================================================================
    // uploadAll: rollback
    // =====================================================================

    @Test
    void uploadAll_shouldNotSaveMetadataWhenStorageFails() {
        doThrow(new StorageException("storage down", null))
                .when(storage).save(anyString(), any(InputStream.class), anyLong(), anyString());

        assertThrows(StorageException.class,
                () -> service.uploadAll(List.of(png("a.png")), null, "seller-1"));

        verify(repo, never()).save(any(Media.class));
        verify(storage, never()).delete(anyString());
    }

    @Test
    void uploadAll_shouldDeleteStoredFileWhenMetadataSaveFails() {
        RuntimeException dbDown = new RuntimeException("db down");
        when(repo.save(any(Media.class))).thenThrow(dbDown);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadAll(List.of(png("a.png")), null, "seller-1"));

        assertSame(dbDown, ex);
        String key = savedKeys(1).get(0);
        verify(storage).delete(key); // no orphan file left in storage
    }

    @Test
    void uploadAll_shouldRollBackEarlierFilesWhenALaterOneFails() {
        when(repo.save(any(Media.class)))
                .thenAnswer(withId("media-1"))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> service.uploadAll(List.of(png("a.png"), png("b.png")), null, "seller-1"));

        List<String> keys = savedKeys(2);
        // second file: cleaned up by store() itself
        verify(storage).delete(keys.get(1));
        // first file: metadata and object both removed by the rollback
        ArgumentCaptor<Media> removed = ArgumentCaptor.forClass(Media.class);
        verify(repo).delete(removed.capture());
        assertEquals("media-1", removed.getValue().getId());
        verify(storage).delete(keys.get(0));
    }

    @Test
    void uploadAll_shouldRethrowOriginalErrorEvenWhenRollbackCleanupFails() {
        StorageException original = new StorageException("second upload failed", null);
        when(repo.save(any(Media.class))).thenAnswer(withId("media-1"));
        doNothing().doThrow(original)
                .when(storage).save(anyString(), any(InputStream.class), anyLong(), anyString());
        doThrow(new StorageException("cleanup failed", null)).when(storage).delete(anyString());

        StorageException ex = assertThrows(StorageException.class,
                () -> service.uploadAll(List.of(png("a.png"), png("b.png")), null, "seller-1"));

        assertSame(original, ex); // the caller sees the real cause, not the cleanup error
        verify(repo).delete(any(Media.class));
    }

    // =====================================================================
    // metadata / content
    // =====================================================================

    @Test
    void metadata_shouldReturnMediaWhenFound() {
        Media m = media("media-1", "product-1", "seller-1");
        when(repo.findById("media-1")).thenReturn(Optional.of(m));

        assertSame(m, service.metadata("media-1"));
    }

    @Test
    void metadata_shouldThrowNotFoundWhenMissing() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        MediaNotFoundException ex = assertThrows(MediaNotFoundException.class,
                () -> service.metadata("missing"));

        assertEquals("Media not found: missing", ex.getMessage());
    }

    @Test
    void content_shouldLoadFromStorageByImagePath() {
        Media m = media("media-1", "product-1", "seller-1");
        InputStream stream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        when(storage.load("media-1.png")).thenReturn(stream);

        assertSame(stream, service.content(m));
    }

    // =====================================================================
    // delete
    // =====================================================================

    @Test
    void delete_shouldRemoveMetadataAndFileWhenOwned() {
        Media m = media("media-1", "product-1", "seller-1");
        when(repo.findById("media-1")).thenReturn(Optional.of(m));

        service.delete("media-1", "seller-1");

        verify(repo).delete(m);
        verify(storage).delete("media-1.png");
    }

    @Test
    void delete_shouldThrowNotFoundAndKeepEverythingWhenNotOwned() {
        when(repo.findById("media-1")).thenReturn(Optional.of(media("media-1", "product-1", "seller-2")));

        MediaNotFoundException ex = assertThrows(MediaNotFoundException.class,
                () -> service.delete("media-1", "seller-1"));

        // same message as a missing media: never reveal that it exists
        assertEquals("Media not found: media-1", ex.getMessage());
        verify(repo, never()).delete(any(Media.class));
        verifyNoInteractions(storage);
    }

    @Test
    void delete_shouldThrowNotFoundWhenMissing() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        assertThrows(MediaNotFoundException.class, () -> service.delete("missing", "seller-1"));

        verifyNoInteractions(storage);
    }

    @Test
    void delete_shouldSucceedEvenWhenStorageDeleteFails() {
        Media m = media("media-1", "product-1", "seller-1");
        when(repo.findById("media-1")).thenReturn(Optional.of(m));
        doThrow(new StorageException("storage down", null)).when(storage).delete("media-1.png");

        assertDoesNotThrow(() -> service.delete("media-1", "seller-1"));

        verify(repo).delete(m);
    }

    // =====================================================================
    // urlsByProduct
    // =====================================================================

    @Test
    void urlsByProduct_shouldGroupUrlsAndKeepProductsWithoutImages() {
        when(repo.findByProductIdIn(anyCollection(), eq(Sort.by("createdAt")))).thenReturn(List.of(
                media("m1", "product-1", "seller-1"),
                media("m2", "product-1", "seller-1"),
                media("m3", "product-2", "seller-1")));

        Map<String, List<String>> result = service.urlsByProduct(List.of("product-1", "product-2", "product-3"));

        assertEquals(List.of("/media/images/m1", "/media/images/m2"), result.get("product-1"));
        assertEquals(List.of("/media/images/m3"), result.get("product-2"));
        assertEquals(List.of(), result.get("product-3"));
        // keys keep the order the caller asked for
        assertEquals(List.of("product-1", "product-2", "product-3"), List.copyOf(result.keySet()));
    }

    @Test
    void urlsByProduct_shouldQueryOnceWithDistinctIdsSortedByCreationDate() {
        when(repo.findByProductIdIn(anyCollection(), eq(Sort.by("createdAt")))).thenReturn(List.of());

        Map<String, List<String>> result = service.urlsByProduct(Arrays.asList("product-1", "product-1"));

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(repo, times(1)).findByProductIdIn(ids.capture(), eq(Sort.by("createdAt")));
        assertEquals(List.of("product-1"), List.copyOf(ids.getValue()));
    }

    // =====================================================================
    // deleteByProduct
    // =====================================================================

    @Test
    void deleteByProduct_shouldRemoveEveryImageOfTheCaller() {
        Media m1 = media("m1", "product-1", "seller-1");
        Media m2 = media("m2", "product-1", "seller-1");
        when(repo.findByProductIdAndOwnerId("product-1", "seller-1")).thenReturn(List.of(m1, m2));

        service.deleteByProduct("product-1", "seller-1");

        verify(repo).delete(m1);
        verify(repo).delete(m2);
        verify(storage).delete("m1.png");
        verify(storage).delete("m2.png");
    }

    @Test
    void deleteByProduct_shouldDoNothingWhenCallerHasNoImages() {
        when(repo.findByProductIdAndOwnerId("product-1", "seller-1")).thenReturn(List.of());

        service.deleteByProduct("product-1", "seller-1");

        verify(repo, never()).delete(any(Media.class));
        verifyNoInteractions(storage);
    }
}