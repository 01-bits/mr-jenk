package com.api.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.api.product.model.Product;

/**
 * ProductRepository
 */
public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByUserId(String userId, Pageable pageable);
}
