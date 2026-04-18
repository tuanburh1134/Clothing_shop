package com.example.shop.dto;

public class ProductVariant {

    private String color;
    private Integer s = 0;
    private Integer m = 0;
    private Integer l = 0;
    private Integer xl = 0;
    private Integer xxl = 0;

    public ProductVariant() {
    }

    public ProductVariant(String color, Integer s, Integer m, Integer l, Integer xl, Integer xxl) {
        this.color = color;
        this.s = defaultValue(s);
        this.m = defaultValue(m);
        this.l = defaultValue(l);
        this.xl = defaultValue(xl);
        this.xxl = defaultValue(xxl);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getS() {
        return s;
    }

    public void setS(Integer s) {
        this.s = defaultValue(s);
    }

    public Integer getM() {
        return m;
    }

    public void setM(Integer m) {
        this.m = defaultValue(m);
    }

    public Integer getL() {
        return l;
    }

    public void setL(Integer l) {
        this.l = defaultValue(l);
    }

    public Integer getXl() {
        return xl;
    }

    public void setXl(Integer xl) {
        this.xl = defaultValue(xl);
    }

    public Integer getXxl() {
        return xxl;
    }

    public void setXxl(Integer xxl) {
        this.xxl = defaultValue(xxl);
    }

    public int getTotalQuantity() {
        return defaultValue(s) + defaultValue(m) + defaultValue(l) + defaultValue(xl) + defaultValue(xxl);
    }

    private Integer defaultValue(Integer value) {
        return value == null ? 0 : value;
    }
}
