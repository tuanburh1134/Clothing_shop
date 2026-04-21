package com.example.shop.repository;

import com.example.shop.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findAllByProductIdOrderByCreatedAtDesc(Long productId);

    List<ProductReview> findAllByUsernameOrderByCreatedAtDesc(String username);

    boolean existsByOrderItemIdAndUsername(Long orderItemId, String username);
}
