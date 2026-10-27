package com.api.media_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.api.media_service.client.ProductClient;
import com.api.media_service.dto.MediaResponse;
import com.api.media_service.exception.InvalidMediaException;
import com.api.media_service.exception.MediaNotFoundException;
import com.api.media_service.model.Media;
import com.api.media_service.repository.MediaRepository;
import com.api.media_service.storage.StorageException;
import com.api.media_service.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final int MAX_MAGIC_SIZE = 12;
    private static final int MAX_FILES = 5;

    private final MediaRepository repo;
    private final StorageService storage;
    private final ProductClient productClient;

    private record Prepared(MultipartFile file, ImageSniffer.Detected type) {
    }

    public List<MediaResponse> uploadAll(List<MultipartFile> files, String productId, String ownerId) {
        if (files == null || files.isEmpty()) {
            throw new InvalidMediaException("At least one file is required");
        }
        if (files.size() > MAX_FILES) {
            throw new InvalidMediaException("Too many files (max " + MAX_FILES + ")");
        }
        String pid = (productId == null || productId.isBlank()) ? null : productId.trim();

        // 1. validate every file first, so a bad file never leaves a partial upload
        List<Prepared> prepared = new ArrayList<>();
        for (MultipartFile file : files) {
            prepared.add(validate(file));
        }

        // 2. one ownership check for the whole request
        if (pid != null) {
            productClient.assertOwnedBy(pid, ownerId);
        }

        // 3. store, and undo everything if one file fails
        List<Media> saved = new ArrayList<>();
        try {
            for (Prepared p : prepared) {
                saved.add(store(p, pid, ownerId));
            }
        } catch (RuntimeException e) {
            saved.forEach(this::remove);
            throw e;
        }
        return saved.stream().map(MediaResponse::from).toList();
    }

    private Prepared validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("File is required");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidMediaException("File too large (max 2 MB)");
        }
        String declared = file.getContentType();
        if (declared == null || !declared.toLowerCase().startsWith("image/")) {
            throw new InvalidMediaException("Only image files are allowed");
        }

        ImageSniffer.Detected type;
        try (InputStream in = file.getInputStream()) {
            type = ImageSniffer.detect(in.readNBytes(MAX_MAGIC_SIZE));
        } catch (IOException e) {
            throw new StorageException("Cannot read the uploaded file", e);
        }
        if (type == null) {
            throw new InvalidMediaException("Unsupported image content (allowed: jpeg, png, gif, webp)");
        }
        return new Prepared(file, type);
    }

    private Media store(Prepared p, String productId, String ownerId) {
        String key = UUID.randomUUID() + "." + p.type().extension(); // client filename is never used
        try (InputStream in = p.file().getInputStream()) {
            storage.save(key, in, p.file().getSize(), p.type().contentType());
        } catch (IOException e) {
            throw new StorageException("Cannot upload the file", e);
        }

        Media media = new Media();
        media.setImagePath(key);
        media.setOwnerId(ownerId);
        media.setProductId(productId);
        media.setContentType(p.type().contentType());
        media.setSize(p.file().getSize());
        try {
            return repo.save(media);
        } catch (RuntimeException e) {
            storage.delete(key); // do not leave an orphan file behind
            throw e;
        }
    }

    private void remove(Media m) {
        repo.delete(m);
        try {
            storage.delete(m.getImagePath());
        } catch (StorageException e) {
            log.warn("Rollback: could not remove object {}", m.getImagePath(), e);
        }
    }

    public Media metadata(String id) {
        return repo.findById(id).orElseThrow(() -> new MediaNotFoundException(id));
    }

    public InputStream content(Media m) {
        return storage.load(m.getImagePath());
    }

    public void delete(String id, String ownerId) {
        Media m = metadata(id);
        if (!m.getOwnerId().equals(ownerId)) {
            throw new MediaNotFoundException(id); // not owned is reported as 404
        }
        remove(m);
    }

    public Map<String, List<String>> urlsByProduct(Collection<String> productIds) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        productIds.forEach(id -> result.put(id, new ArrayList<>()));
        repo.findByProductIdIn(result.keySet(), Sort.by("createdAt"))
                .forEach(m -> result.get(m.getProductId()).add("/media/images/" + m.getId()));
        return result;
    }

    // deletes only the caller's own images of that product
    public void deleteByProduct(String productId, String ownerId) {
        repo.findByProductIdAndOwnerId(productId, ownerId).forEach(this::remove);
    }
}