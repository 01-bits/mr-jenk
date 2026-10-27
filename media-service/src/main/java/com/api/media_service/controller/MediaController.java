package com.api.media_service.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.api.media_service.dto.MediaResponse;
import com.api.media_service.exception.InvalidMediaException;
import com.api.media_service.model.Media;
import com.api.media_service.service.MediaService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * MediaController
 */
@RestController
@RequestMapping("/media/images")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService service;

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<MediaResponse> upload(@RequestParam() List<MultipartFile> files,
            @RequestParam(required = false) String productId,
            Authentication auth) {
        return service.uploadAll(files, productId, auth.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> get(@PathVariable String id, WebRequest req) {
        Media media = service.metadata(id);
        String etag = "\"" + media.getId() + "\"";
        if (req.checkNotModified(etag)) {
            return null; // 304
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .contentLength(media.getSize())
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(service.content(media)));
    }

    @GetMapping // GET /media/images?productIds=a&productIds=b
    public Map<String, List<String>> byProducts(@RequestParam List<String> productIds) {
        if (productIds.size() > 100) {
            throw new InvalidMediaException("Too many product ids (max 100)");
        }
        return service.urlsByProduct(productIds);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping // DELETE /media/images?productId=a
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByProduct(@RequestParam String productId, Authentication auth) {
        service.deleteByProduct(productId, auth.getName());
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, Authentication auth) {
        service.delete(id, auth.getName());
    }

}