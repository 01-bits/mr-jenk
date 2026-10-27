package com.api.media_service.service;

/**
 * ImageSniffer
 */
public class ImageSniffer {
    private ImageSniffer() {}

    public record Detected(String contentType, String extension) {}

    public static Detected detect(byte[] b) {
        if (startsWith(b, 0, 0xFF, 0xD8, 0xFF))
            return new Detected("image/jpeg", "jpg");
        if (startsWith(b, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            return new Detected("image/png", "png");
        if (startsWith(b, 0, 'G', 'I', 'F', '8') && b.length >= 6
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a')
            return new Detected("image/gif", "gif");
        if (startsWith(b, 0, 'R', 'I', 'F', 'F') && startsWith(b, 8, 'W', 'E', 'B', 'P'))
            return new Detected("image/webp", "webp");
        return null;
    }

    private static boolean startsWith(byte[] b, int offset, int... sig) {
        if (b.length < offset + sig.length)
            return false;
        for (int i = 0; i < sig.length; i++) {
            if ((b[offset + i] & 0xFF) != sig[i]) {
                return false;
            }
        }
        return true;
    }
}
