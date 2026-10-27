package com.api.media_service.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Media
 */
@Document("media")
@Getter
@Setter
@NoArgsConstructor
public class Media {
    @Id
    private String id;
    private String imagePath;
    @Indexed
    private String productId;
    @Indexed
    private String ownerId;
    private String contentType;
    private long size;
    @CreatedDate
    private Instant createdAt;
}
