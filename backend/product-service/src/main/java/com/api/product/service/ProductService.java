package com.api.product.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.api.product.client.MediaClient;
import com.api.product.dto.ProductRequest;
import com.api.product.dto.ProductResponse;
import com.api.product.exception.ProductNotFoundException;
import com.api.product.model.Product;
import com.api.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * ProductService
 */
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final MediaClient mediaClient;

    public ProductResponse create(ProductRequest req, String sellerId) {
        Product p = new Product();
        p.setUserId(sellerId);
        apply(p, req);
        return ProductResponse.from(repo.save(p), List.of());
    }

    public Page<ProductResponse> list(Pageable pageable) {
        return withImages(repo.findAll(pageable));
    }

    public Page<ProductResponse> listMine(String userId, Pageable pageable) {
        return withImages(repo.findByUserId(userId, pageable));
    }

    public ProductResponse get(String id) {
        return one(find(id));
    }

    public ProductResponse update(String id, ProductRequest req, String userId) {
        Product p = findOwned(id, userId);
        apply(p, req);
        return one(repo.save(p));
    }

    public void delete(String id, String userId) {
        Product p = findOwned(id, userId);
        repo.delete(p);
        mediaClient.deleteImagesOf(p.getId(), userId);
    }

    public String ownerOf(String id) {
        return find(id).getUserId();
    }

    private Product find(String id) {
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product findOwned(String id, String sellerId) {
        Product p = find(id);
        if (!p.getUserId().equals(sellerId)) {
            throw new ProductNotFoundException(id);
        }
        return p;
    }

    private ProductResponse one(Product p) {
        Map<String, List<String>> images = mediaClient.imageUrls(List.of(p.getId()));
        return ProductResponse.from(p, urlsOf(images, p.getId()));
    }

    private Page<ProductResponse> withImages(Page<Product> page) {
        List<String> ids = page.getContent().stream().map(Product::getId).toList();
        Map<String, List<String>> images = mediaClient.imageUrls(ids);
        return page.map(p -> ProductResponse.from(p, urlsOf(images, p.getId())));
    }

    private static List<String> urlsOf(Map<String, List<String>> images, String productId) {
        return images == null ? null : images.getOrDefault(productId, List.of());
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setQuantity(r.quantity());
    }
}