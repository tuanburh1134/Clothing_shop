package com.example.shop.controller;

import com.example.shop.dto.ProductForm;
import com.example.shop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String getAllProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "products/list";
    }

    @GetMapping("/admin/products/new")
    public String showCreateForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        model.addAttribute("isEdit", false);
        return "products/admin-form";
    }

    @PostMapping("/admin/products")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "products/admin-form";
        }

        productService.createProduct(form, imageFile);
        return "redirect:/products";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        var product = productService.getProductById(id);
        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setPrice(product.getPrice());
        form.setQuantity(product.getQuantity());

        model.addAttribute("productId", id);
        model.addAttribute("productForm", form);
        model.addAttribute("existingImageUrl", product.getImageUrl());
        model.addAttribute("isEdit", true);
        return "products/admin-form";
    }

    @PostMapping("/admin/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("isEdit", true);
            model.addAttribute("existingImageUrl", productService.getProductById(id).getImageUrl());
            return "products/admin-form";
        }

        productService.updateProduct(id, form, imageFile);
        return "redirect:/products";
    }

    @PostMapping("/admin/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/products";
    }
}