package com.example.shop.controller;

import com.example.shop.dto.CartItemView;
import com.example.shop.dto.ProductCategoryCardView;
import com.example.shop.dto.ProductForm;
import com.example.shop.entity.Product;
import com.example.shop.entity.ProductCategory;
import com.example.shop.exception.BadRequestException;
import com.example.shop.service.ProductService;
import jakarta.servlet.http.HttpSession;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductController {

    private static final String CART_SESSION_KEY = "SHOP_CART";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String getAllProducts(Model model) {
        model.addAttribute("hotProducts", productService.getHotProducts());
        model.addAttribute("newestProducts", productService.getNewestProducts());
        model.addAttribute("categoryCards", buildCategoryCards());
        return "products/list";
    }

    @GetMapping("/products/all")
    public String getAllProductsNewestFirst(Model model) {
        model.addAttribute("products", productService.getAllProductsNewestFirst());
        model.addAttribute("pageTitle", "Tất cả sản phẩm (mới nhất -> cũ nhất)");
        return "products/all";
    }

    @GetMapping("/products/category/{categoryKey}")
    public String getProductsByCategory(@PathVariable String categoryKey, Model model) {
        ProductCategory category = resolveCategoryKey(categoryKey);
        model.addAttribute("products", productService.getProductsByCategory(category));
        model.addAttribute("pageTitle", "Danh mục: " + category.getLabel() + " (mới nhất -> cũ nhất)");
        return "products/all";
    }

    @GetMapping("/products/{id}")
    public String getProductDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        List<String> galleryImages = product.getDetailImageList();
        if (galleryImages.isEmpty()) {
            galleryImages = List.of("/images/anhnen.png");
        }

        model.addAttribute("product", product);
        model.addAttribute("galleryImages", galleryImages);
        return "products/detail";
    }

    @PostMapping("/products/{id}/cart")
    public String addToCart(@PathVariable Long id,
                            @RequestParam(name = "redirectTo", required = false) String redirectTo,
                            HttpSession session) {
        productService.getProductById(id);

        Map<Long, Integer> cart = getCart(session);
        cart.merge(id, 1, Integer::sum);
        session.setAttribute(CART_SESSION_KEY, cart);

        return "redirect:" + appendQuery(resolveRedirect(redirectTo), "cartAdded");
    }

    @PostMapping("/products/{id}/buy-now")
    public String buyNow(@PathVariable Long id) {
        productService.getProductById(id);
        return "redirect:/checkout/buy-now?productId=" + id;
    }

    @GetMapping("/cart")
    public String cartPage(HttpSession session, Model model) {
        List<CartItemView> cartItems = buildCartItems(session);
        BigDecimal total = cartItems.stream()
                .map(CartItemView::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", total);
        return "products/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkoutSelectedLegacy() {
        // Forward old cart action to the new checkout workflow.
        return "forward:/checkout/cart";
    }

    @GetMapping("/admin/products/new")
    public String showCreateForm(Model model) {
        ProductForm form = new ProductForm();
        form.setDiscountPercent(0);
        model.addAttribute("productForm", form);
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("formAction", "/admin/products");
        model.addAttribute("existingImageUrl", null);
        model.addAttribute("existingDetailImages", List.of());
        return "admin/inventory-form";
    }

    @PostMapping("/admin/products")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "detailImageFiles", required = false) MultipartFile[] detailImageFiles,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("formAction", "/admin/products");
            model.addAttribute("existingImageUrl", null);
            model.addAttribute("existingDetailImages", List.of());
            return "admin/inventory-form";
        }

        try {
            productService.createProduct(form, imageFile, detailImageFiles);
        } catch (BadRequestException ex) {
            model.addAttribute("formError", ex.getMessage());
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("formAction", "/admin/products");
            model.addAttribute("existingImageUrl", null);
            model.addAttribute("existingDetailImages", List.of());
            return "admin/inventory-form";
        }

        return "redirect:/admin/inventory";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setShortDescription(product.getShortDescription());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setQuantity(product.getQuantity());
        form.setVariantData(product.getVariantData());
        form.setDiscountPercent(product.getDiscountPercent());
        form.setExistingDetailImageUrls(product.getDetailImageUrls());
        if (product.getCategory() != null) {
            form.setCategory(product.getCategory().name());
        }

        model.addAttribute("productId", id);
        model.addAttribute("productForm", form);
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("existingImageUrl", product.getImageUrl());
        model.addAttribute("existingDetailImages", product.getDetailImageList());
        model.addAttribute("isEdit", true);
        model.addAttribute("formAction", "/admin/products/" + id);
        return "admin/inventory-form";
    }

    @PostMapping("/admin/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "detailImageFiles", required = false) MultipartFile[] detailImageFiles,
                                Model model) {
        if (bindingResult.hasErrors()) {
            Product product = productService.getProductById(id);
            model.addAttribute("productId", id);
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("existingImageUrl", product.getImageUrl());
            model.addAttribute("existingDetailImages", product.getDetailImageList());
            model.addAttribute("formAction", "/admin/products/" + id);
            return "admin/inventory-form";
        }

        try {
            productService.updateProduct(id, form, imageFile, detailImageFiles);
        } catch (BadRequestException ex) {
            Product product = productService.getProductById(id);
            model.addAttribute("formError", ex.getMessage());
            model.addAttribute("productId", id);
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("existingImageUrl", product.getImageUrl());
            model.addAttribute("existingDetailImages", product.getDetailImageList());
            model.addAttribute("formAction", "/admin/products/" + id);
            return "admin/inventory-form";
        }

        return "redirect:/admin/inventory";
    }

    @PostMapping("/admin/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/admin/inventory";
    }

    private String resolveRedirect(String redirectTo) {
        if (redirectTo != null && redirectTo.startsWith("/products")) {
            return redirectTo;
        }
        return "/products";
    }

    private String appendQuery(String path, String key) {
        return path + (path.contains("?") ? "&" : "?") + key;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<Long, Integer> casted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof Long id && entry.getValue() instanceof Integer qty && qty > 0) {
                    casted.put(id, qty);
                }
            }
            return casted;
        }
        return new LinkedHashMap<>();
    }

    private List<CartItemView> buildCartItems(HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        List<CartItemView> items = new ArrayList<>();

        Iterator<Map.Entry<Long, Integer>> iterator = cart.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Integer> entry = iterator.next();
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            try {
                Product product = productService.getProductById(productId);
                CartItemView item = new CartItemView();
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setShortDescription(product.getShortDescription());
                item.setImageUrl(product.getImageUrl());
                item.setUnitPrice(product.getDiscountedPrice());
                item.setQuantity(quantity);
                item.setLineTotal(product.getDiscountedPrice().multiply(BigDecimal.valueOf(quantity)));
                items.add(item);
            } catch (BadRequestException ex) {
                iterator.remove();
            }
        }

        session.setAttribute(CART_SESSION_KEY, cart);
        return items;
    }

    private List<ProductCategoryCardView> buildCategoryCards() {
        return List.of(
                new ProductCategoryCardView("Quần Nam", "/images/quannam.jpg", "/products/category/quannam"),
                new ProductCategoryCardView("Áo Nam", "/images/aonam.jpg", "/products/category/aonam"),
                new ProductCategoryCardView("Quần Nữ", "/images/quannu.jpg", "/products/category/quannu"),
                new ProductCategoryCardView("Áo Nữ", "/images/aonu.jpg", "/products/category/aonu")
        );
    }

    private ProductCategory resolveCategoryKey(String categoryKey) {
        return switch (categoryKey.toLowerCase()) {
            case "quannam" -> ProductCategory.QUAN_NAM;
            case "aonam" -> ProductCategory.AO_NAM;
            case "quannu" -> ProductCategory.QUAN_NU;
            case "aonu" -> ProductCategory.AO_NU;
            default -> throw new BadRequestException("Danh mục không tồn tại");
        };
    }
}