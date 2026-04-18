package com.example.shop.dto;

public class ProductCategoryCardView {

    private final String title;
    private final String imageUrl;
    private final String targetUrl;

    public ProductCategoryCardView(String title, String imageUrl, String targetUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }
}
