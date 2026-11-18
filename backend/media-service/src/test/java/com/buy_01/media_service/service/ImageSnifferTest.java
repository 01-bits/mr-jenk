package com.buy_01.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.api.media_service.service.ImageSniffer;
import com.api.media_service.service.ImageSniffer.Detected;

/**
 * ImageSnifferTest
 *
 * Proves that the type is decided by the file's first bytes (its "magic
 * number"), never by the name or the declared content type.
 */
public class ImageSnifferTest {

    // ---------- helpers ----------

    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    // ---------- recognised formats ----------
    @Test
    void detect_shouldRecognizeJpeg() {
        byte[] jpeng = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10);

        assertEquals(new Detected("image/jpeg", "jpg"), ImageSniffer.detect(jpeng));
    }

    @Test
    void detect_shouldRecognizePng() {
        byte[] png = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00);

        assertEquals(new Detected("image/png", "png"), ImageSniffer.detect(png));
    }

    @Test
    void detect_shouldRecognizeGif87a() {
        assertEquals(new Detected("image/gif", "gif"), ImageSniffer.detect(ascii("GIF87a")));
    }

    @Test
    void detect_shouldRecognizeGif89a() {
        assertEquals(new Detected("image/gif", "gif"), ImageSniffer.detect(ascii("GIF89a")));
    }

    @Test
    void detect_shouldRecognizeWebp() {
        // RIFF, then 4 bytes of file size (any value), then WEBP
        byte[] webp = bytes('R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P');

        assertEquals(new Detected("image/webp", "webp"), ImageSniffer.detect(webp));
    }

    // ---------- rejected content ----------

    @Test
    void detect_shouldRejectUnknownContent() {
        assertNull(ImageSniffer.detect(ascii("hello world!")));
    }

    @Test
    void detect_shouldRejectExecutableDisguisedAsImage() {
        // "MZ" is the start of a Windows .exe; the file name is irrelevant here
        assertNull(ImageSniffer.detect(bytes('M', 'Z', 0x90, 0x00, 0x03, 0x00)));
    }

    @Test
    void detect_shouldRejectRiffWithoutWebp() {
        // a WAV audio file is also RIFF, but WAVE instead of WEBP
        byte[] wav = bytes('R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'A', 'V', 'E');

        assertNull(ImageSniffer.detect(wav));
    }

    @Test
    void detect_shouldRejectGifWithUnknownVersion() {
        assertNull(ImageSniffer.detect(ascii("GIF88a")));
    }

    // ---------- short input: must return null, never throw ----------

    @Test
    void detect_shouldRejectEmptyArray() {
        assertNull(ImageSniffer.detect(new byte[0]));
    }

    @Test
    void detect_shouldRejectTruncatedJpegSignature() {
        assertNull(ImageSniffer.detect(bytes(0xFF, 0xD8)));
    }

    @Test
    void detect_shouldRejectTruncatedPngSignature() {
        assertNull(ImageSniffer.detect(bytes(0x89, 0x50, 0x4E, 0x47)));
    }

    @Test
    void detect_shouldRejectTruncatedGif() {
        // "GIF8" matches the first check, but the version bytes are missing
        assertNull(ImageSniffer.detect(ascii("GIF8")));
    }

    @Test
    void detect_shouldRejectRiffShorterThanWebpHeader() {
        assertNull(ImageSniffer.detect(ascii("RIFF")));
    }

}