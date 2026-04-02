package com.example.shop.service.impl;

import com.example.shop.dto.ProductForm;
import com.example.shop.entity.Product;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.ProductService;
import com.example.shop.util.ImageStorageUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ImageStorageUtil imageStorageUtil;

    public ProductServiceImpl(ProductRepository productRepository, ImageStorageUtil imageStorageUtil) {
        this.productRepository = productRepository;
        this.imageStorageUtil = imageStorageUtil;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product createProduct(ProductForm form, MultipartFile imageFile) {
        Product product = new Product(form.getName().trim(), form.getPrice(), form.getQuantity());
        product.setImageUrl(imageStorageUtil.storeProductImage(imageFile));
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public Product updateProduct(Long id, ProductForm form, MultipartFile imageFile) {
        Product product = getProductById(id);
        product.setName(form.getName().trim());
        product.setPrice(form.getPrice());
        product.setQuantity(form.getQuantity());

        String newImageUrl = imageStorageUtil.storeProductImage(imageFile);
        if (newImageUrl != null) {
            imageStorageUtil.deleteProductImage(product.getImageUrl());
            product.setImageUrl(newImageUrl);
        }

        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        imageStorageUtil.deleteProductImage(product.getImageUrl());
        productRepository.delete(product);
    }
}