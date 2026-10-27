package com.api.media_service.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.api.media_service.model.Media;

/**
 * MediaRepository
 */
public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByProductIdIn(Collection<String> productIds, Sort sort);

    List<Media> findByProductIdAndOwnerId(String productId, String ownerId);
}
