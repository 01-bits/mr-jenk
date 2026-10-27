package com.buy01.user.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.buy01.user.DTO.MediaResponse;
import com.buy01.user.config.FeignSupportConfig;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@FeignClient(name = "media-service", configuration = FeignSupportConfig.class)
public interface MediaService {
    @PostMapping(value = "/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<MediaResponse> upload(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) String productId);

}