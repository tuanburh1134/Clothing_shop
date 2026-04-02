package com.example.shop.service;

import com.example.shop.dto.ProductForm;
import com.example.shop.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();

    Product createProduct(ProductForm form, MultipartFile imageFile);

    Product getProductById(Long id);

    Product updateProduct(Long id, ProductForm form, MultipartFile imageFile);

    void deleteProduct(Long id);
}