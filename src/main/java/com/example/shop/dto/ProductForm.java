package com.example.shop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductForm {

    @NotBlank(message = "Tên sản phẩm là bắt buộc")
    @Size(max = 150, message = "Tên sản phẩm phải tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Mô tả ngắn là bắt buộc")
    @Size(max = 255, message = "Mô tả ngắn phải tối đa 255 ký tự")
    private String shortDescription;

    @NotBlank(message = "Mô tả chi tiết là bắt buộc")
    private String description;

    @NotBlank(message = "Danh mục là bắt buộc")
    private String category;

    @NotNull(message = "Giá bán là bắt buộc")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho là bắt buộc")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity = 0;

    @NotBlank(message = "Vui lòng nhập ít nhất một màu sắc")
    private String variantData;

    @Min(value = 0, message = "Giảm giá không được nhỏ hơn 0")
    @Max(value = 99, message = "Giảm giá tối đa là 99%")
    private Integer discountPercent = 0;

    private String existingDetailImageUrls;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getVariantData() {
        return variantData;
    }

    public void setVariantData(String variantData) {
        this.variantData = variantData;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public String getExistingDetailImageUrls() {
        return existingDetailImageUrls;
    }

    public void setExistingDetailImageUrls(String existingDetailImageUrls) {
        this.existingDetailImageUrls = existingDetailImageUrls;
    }
}
