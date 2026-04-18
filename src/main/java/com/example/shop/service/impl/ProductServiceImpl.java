package com.example.shop.service.impl;

import com.example.shop.dto.ProductForm;
import com.example.shop.entity.Product;
import com.example.shop.entity.ProductCategory;
import com.example.shop.exception.BadRequestException;
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
    public List<Product> getHotProducts() {
        return productRepository.findTop4ByOrderByPurchaseCountDescCreatedAtDesc();
    }

    @Override
    public List<Product> getNewestProducts() {
        return productRepository.findTop4ByOrderByCreatedAtDesc();
    }

    @Override
    public List<Product> getAllProductsNewestFirst() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Product> getProductsByCategory(ProductCategory category) {
        return productRepository.findAllByCategoryOrderByCreatedAtDesc(category);
    }

    @Override
    public Product createProduct(ProductForm form, MultipartFile imageFile, MultipartFile[] detailImageFiles) {
        Product product = new Product();
        applyForm(product, form, imageFile, detailImageFiles, true);
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public Product updateProduct(Long id, ProductForm form, MultipartFile imageFile, MultipartFile[] detailImageFiles) {
        Product product = getProductById(id);
        applyForm(product, form, imageFile, detailImageFiles, false);
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        imageStorageUtil.deleteProductImage(product.getImageUrl());
        imageStorageUtil.deleteProductImages(product.getDetailImageList());
        productRepository.delete(product);
    }

    @Override
    public void registerPurchase(Long productId, int quantity) {
        if (quantity <= 0) {
            return;
        }

        Product product = getProductById(productId);
        product.setPurchaseCount(product.getPurchaseCount() + quantity);
        productRepository.save(product);
    }

    private void applyForm(Product product, ProductForm form, MultipartFile imageFile, MultipartFile[] detailImageFiles, boolean isCreate) {
        product.setName(form.getName().trim());
        product.setShortDescription(form.getShortDescription().trim());
        product.setDescription(form.getDescription().trim());
        product.setCategory(resolveCategory(form.getCategory()));
        product.setPrice(form.getPrice());
        product.setQuantity(resolveQuantity(form));
        product.setVariantData(form.getVariantData());
        product.setDiscountPercent(form.getDiscountPercent() == null ? 0 : form.getDiscountPercent());

        String newImageUrl = imageStorageUtil.storeProductImage(imageFile);
        if (newImageUrl != null) {
            if (!isCreate) {
                imageStorageUtil.deleteProductImage(product.getImageUrl());
            }
            product.setImageUrl(newImageUrl);
        }

        String detailImageUrls = imageStorageUtil.storeProductImages(detailImageFiles);
        if (detailImageUrls != null) {
            if (!isCreate) {
                imageStorageUtil.deleteProductImages(product.getDetailImageList());
            }
            product.setDetailImageUrls(detailImageUrls);
        } else if (isCreate) {
            product.setDetailImageUrls(null);
        } else {
            product.setDetailImageUrls(form.getExistingDetailImageUrls());
        }
    }

    private Integer resolveQuantity(ProductForm form) {
        int calculated = 0;

        if (form.getVariantData() != null && !form.getVariantData().isBlank()) {
            String[] variants = form.getVariantData().split(";;");
            for (String variant : variants) {
                String[] blocks = variant.split("::", 2);
                if (blocks.length < 2) {
                    continue;
                }

                for (String sizeEntry : blocks[1].split(",")) {
                    String[] pair = sizeEntry.split("=");
                    if (pair.length == 2) {
                        try {
                            calculated += Integer.parseInt(pair[1].trim());
                        } catch (NumberFormatException ignored) {
                            // Ignore malformed quantities and keep processing.
                        }
                    }
                }
            }
        }

        if (calculated > 0) {
            return calculated;
        }

        return form.getQuantity() == null ? 0 : form.getQuantity();
    }

    private ProductCategory resolveCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            throw new BadRequestException("Danh mục là bắt buộc");
        }

        try {
            return ProductCategory.valueOf(rawCategory.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Danh mục không hợp lệ");
        }
    }
}