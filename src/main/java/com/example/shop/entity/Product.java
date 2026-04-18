package com.example.shop.entity;

import com.example.shop.dto.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "detail_image_urls", columnDefinition = "TEXT")
    private String detailImageUrls;

    @Column(name = "variant_data", columnDefinition = "TEXT")
    private String variantData;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private ProductCategory category;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "purchase_count")
    private Integer purchaseCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Product() {
    }

    public Product(String name, BigDecimal price, Integer quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDetailImageUrls() {
        return detailImageUrls;
    }

    public void setDetailImageUrls(String detailImageUrls) {
        this.detailImageUrls = detailImageUrls;
    }

    public String getVariantData() {
        return variantData;
    }

    public void setVariantData(String variantData) {
        this.variantData = variantData;
    }

    public Integer getDiscountPercent() {
        return discountPercent == null ? 0 : discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public Integer getPurchaseCount() {
        return purchaseCount == null ? 0 : purchaseCount;
    }

    public void setPurchaseCount(Integer purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Transient
    public BigDecimal getDiscountedPrice() {
        if (price == null) {
            return BigDecimal.ZERO;
        }

        int discount = getDiscountPercent();
        if (discount <= 0) {
            return price;
        }

        BigDecimal factor = BigDecimal.valueOf(100 - discount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public List<String> getDetailImageList() {
        List<String> images = new ArrayList<>();

        if (imageUrl != null && !imageUrl.isBlank()) {
            images.add(imageUrl);
        }

        if (detailImageUrls != null && !detailImageUrls.isBlank()) {
            for (String image : detailImageUrls.split("\\|\\|")) {
                if (image != null && !image.isBlank() && !images.contains(image)) {
                    images.add(image);
                }
            }
        }

        return images;
    }

    @Transient
    public List<ProductVariant> getVariants() {
        List<ProductVariant> variants = new ArrayList<>();

        if (variantData == null || variantData.isBlank()) {
            return variants;
        }

        for (String variantEntry : variantData.split(";;")) {
            if (variantEntry == null || variantEntry.isBlank()) {
                continue;
            }

            String[] parts = variantEntry.split("::", 2);
            ProductVariant variant = new ProductVariant();
            variant.setColor(parts[0].trim());

            if (parts.length > 1) {
                for (String sizeEntry : parts[1].split(",")) {
                    String[] pair = sizeEntry.split("=");
                    if (pair.length != 2) {
                        continue;
                    }

                    int sizeQuantity = parseNumber(pair[1]);
                    switch (pair[0].trim().toUpperCase()) {
                        case "S" -> variant.setS(sizeQuantity);
                        case "M" -> variant.setM(sizeQuantity);
                        case "L" -> variant.setL(sizeQuantity);
                        case "XL" -> variant.setXl(sizeQuantity);
                        case "XXL" -> variant.setXxl(sizeQuantity);
                        default -> {
                        }
                    }
                }
            }

            variants.add(variant);
        }

        return variants;
    }

    private int parseNumber(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
