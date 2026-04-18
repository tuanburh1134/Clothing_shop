package com.example.shop.controller;

import com.example.shop.dto.CartItemView;
import com.example.shop.dto.ProductCategoryCardView;
import com.example.shop.dto.ProductForm;
import com.example.shop.dto.ProductVariant;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
                            @RequestParam("selectedColor") String selectedColor,
                            @RequestParam("selectedSize") String selectedSize,
                            @RequestParam(name = "redirectTo", required = false) String redirectTo,
                            HttpSession session) {
        Product product = productService.getProductById(id);
        CartSelection selection = resolveSelection(product, selectedColor, selectedSize);

        Map<String, Integer> cart = getCart(session);
        String cartKey = buildCartKey(id, selection.color(), selection.size());
        cart.merge(cartKey, 1, Integer::sum);
        session.setAttribute(CART_SESSION_KEY, cart);

        return "redirect:" + appendQuery(resolveRedirect(redirectTo), "cartAdded");
    }

    @GetMapping("/products/{id}/buy-now")
    public String buyNow(@PathVariable Long id,
                         @RequestParam("selectedColor") String selectedColor,
                         @RequestParam("selectedSize") String selectedSize) {
        Product product = productService.getProductById(id);
        CartSelection selection = resolveSelection(product, selectedColor, selectedSize);
        return "redirect:/checkout/buy-now?productId=" + id
                + "&selectedColor=" + urlEncode(selection.color())
                + "&selectedSize=" + urlEncode(selection.size());
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
    private Map<String, Integer> getCart(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, Integer> casted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof Integer qty && qty > 0) {
                    casted.put(key, qty);
                }
            }
            return casted;
        }
        return new LinkedHashMap<>();
    }

    private List<CartItemView> buildCartItems(HttpSession session) {
        Map<String, Integer> cart = getCart(session);
        List<CartItemView> items = new ArrayList<>();

        Iterator<Map.Entry<String, Integer>> iterator = cart.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            CartSelection selection = parseCartKey(entry.getKey());
            Integer quantity = entry.getValue();

            if (selection == null) {
                iterator.remove();
                continue;
            }

            try {
                Product product = productService.getProductById(selection.productId());
                resolveSelection(product, selection.color(), selection.size());

                CartItemView item = new CartItemView();
                item.setCartKey(entry.getKey());
                item.setProductId(selection.productId());
                item.setName(product.getName());
                item.setShortDescription(product.getShortDescription());
                item.setImageUrl(product.getImageUrl());
                item.setColor(selection.color());
                item.setSize(selection.size());
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

    private CartSelection resolveSelection(Product product, String selectedColor, String selectedSize) {
        String rawColor = clean(selectedColor);
        String rawSize = normalizeSize(selectedSize);

        if (rawColor == null || rawSize == null) {
            throw new BadRequestException("Vui lòng chọn màu sắc và size trước khi mua hàng");
        }

        for (ProductVariant variant : product.getVariants()) {
            String color = clean(variant.getColor());
            if (color == null || !color.equalsIgnoreCase(rawColor)) {
                continue;
            }

            int available = getSizeQuantity(variant, rawSize);
            if (available <= 0) {
                throw new BadRequestException("Size đã chọn hiện đang hết hàng");
            }

            return new CartSelection(product.getId(), color, rawSize);
        }

        throw new BadRequestException("Màu sắc hoặc size không hợp lệ");
    }

    private int getSizeQuantity(ProductVariant variant, String size) {
        return switch (size) {
            case "S" -> number(variant.getS());
            case "M" -> number(variant.getM());
            case "L" -> number(variant.getL());
            case "XL" -> number(variant.getXl());
            case "XXL" -> number(variant.getXxl());
            default -> 0;
        };
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSize(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ROOT);
    }

    private String buildCartKey(Long productId, String color, String size) {
        return productId + "|" + urlEncode(color) + "|" + urlEncode(size);
    }

    private CartSelection parseCartKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String[] parts = key.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }

        try {
            Long productId = Long.parseLong(parts[0]);
            String color = clean(urlDecode(parts[1]));
            String size = normalizeSize(urlDecode(parts[2]));
            if (color == null || size == null) {
                return null;
            }
            return new CartSelection(productId, color, size);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record CartSelection(Long productId, String color, String size) {
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