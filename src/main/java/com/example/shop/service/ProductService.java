package com.example.shop.service;

import com.example.shop.dto.ProductForm;
import com.example.shop.entity.Product;
import com.example.shop.entity.ProductCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();

    List<Product> getHotProducts();

    List<Product> getNewestProducts();

    List<Product> getAllProductsNewestFirst();

    List<Product> getProductsByCategory(ProductCategory category);

    Product createProduct(ProductForm form, MultipartFile imageFile, MultipartFile[] detailImageFiles);

    Product getProductById(Long id);

    Product updateProduct(Long id, ProductForm form, MultipartFile imageFile, MultipartFile[] detailImageFiles);

    void deleteProduct(Long id);

    void registerPurchase(Long productId, int quantity);
}