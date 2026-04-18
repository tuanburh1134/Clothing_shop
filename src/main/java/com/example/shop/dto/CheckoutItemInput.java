package com.example.shop.dto;

import java.io.Serializable;

public class CheckoutItemInput implements Serializable {

    private Long productId;
    private int quantity;

    public CheckoutItemInput() {
    }

    public CheckoutItemInput(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
