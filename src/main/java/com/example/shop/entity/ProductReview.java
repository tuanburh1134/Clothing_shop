package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_reviews")
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", nullable = false, length = 2000)
    private String comment;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Transient
    public String getStarDisplay() {
        int value = rating == null ? 0 : rating;
        if (value < 0) {
            value = 0;
        }
        if (value > 5) {
            value = 5;
        }

        StringBuilder builder = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            builder.append(i < value ? '★' : '☆');
        }
        return builder.toString();
    }

    @Transient
    public List<String> getImageList() {
        List<String> images = new ArrayList<>();

        if (imageUrls == null || imageUrls.isBlank()) {
            return images;
        }

        for (String image : imageUrls.split("\\|\\|")) {
            if (image != null && !image.isBlank()) {
                images.add(image);
            }
        }

        return images;
    }
}
