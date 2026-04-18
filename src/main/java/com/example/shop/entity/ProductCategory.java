package com.example.shop.entity;

public enum ProductCategory {
    QUAN_NAM("Quần nam"),
    AO_NAM("Áo nam"),
    QUAN_NU("Quần nữ"),
    AO_NU("Áo nữ"),
    BO_NAM("Bộ nam"),
    BO_NU("Bộ nữ");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
