package com.example.shop.service;

import com.example.shop.entity.ProductReview;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface ReviewService {
    void createReview(String username,
                      Long orderId,
                      Long orderItemId,
                      Long productId,
                      Integer rating,
                      String comment,
                      MultipartFile[] imageFiles);

    List<ProductReview> getReviewsForProduct(Long productId);

    Set<Long> getReviewedOrderItemIds(String username);
}
