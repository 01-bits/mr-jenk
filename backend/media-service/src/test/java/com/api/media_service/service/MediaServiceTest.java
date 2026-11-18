package com.api.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.api.media_service.client.ProductClient;
import com.api.media_service.exception.InvalidMediaException;
import com.api.media_service.repository.MediaRepository;
import com.api.media_service.storage.StorageService;

/**
 * MediaServiceTest
 */
@ExtendWith(MockitoExtension.class)
public class MediaServiceTest {

    private static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

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

    // @Test 
    // void uploadAll_shouldAcceptFileOfExactlyTwoMegabytes() {
    //     service.uploadAll(file(null, PNG_SIGNATURE), null, null)
    // }

}