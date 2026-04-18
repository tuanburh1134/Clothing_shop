package com.example.shop.controller;

import com.example.shop.entity.Product;
import com.example.shop.exception.BadRequestException;
import com.example.shop.service.NotificationService;
import com.example.shop.service.OrderService;
import com.example.shop.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public AdminController(ProductService productService,
                           OrderService orderService,
                           NotificationService notificationService) {
        this.productService = productService;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        List<Product> products = productService.getAllProducts();

        long lowStockCount = products.stream()
                .filter(product -> product.getQuantity() != null && product.getQuantity() <= 5)
                .count();

        int totalQuantity = products.stream()
                .map(Product::getQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();

        model.addAttribute("productCount", products.size());
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("totalQuantity", totalQuantity);
        return "admin/dashboard";
    }

    @GetMapping("/admin/inventory")
    public String inventory(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "admin/inventory";
    }

    @GetMapping("/admin/revenue")
    public String revenue(Model model) {
        model.addAttribute("activePage", "revenue");
        return "admin/revenue";
    }

    @GetMapping("/admin/orders")
    public String orders(Model model) {
        model.addAttribute("activePage", "orders");
        model.addAttribute("orders", orderService.getAllOrdersForAdmin());
        return "admin/orders";
    }

    @GetMapping("/admin/notifications")
    public String notifications(Model model) {
        model.addAttribute("activePage", "notifications");
        model.addAttribute("notifications", notificationService.getAdminNotifications());
        return "admin/notifications";
    }

    @PostMapping("/admin/orders/{id}/approve")
    public String approveOrder(@PathVariable Long id) {
        var order = orderService.approveOrder(id);
        notificationService.notifyUserOrderApproved(order.getUsername(), order.getId());
        return "redirect:/admin/orders?approved";
    }

    @PostMapping("/admin/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam("cancelReason") String cancelReason) {
        if (cancelReason == null || cancelReason.trim().isEmpty()) {
            throw new BadRequestException("Vui lòng nhập lí do hủy đơn");
        }

        var order = orderService.cancelOrder(id, cancelReason);
        notificationService.notifyUserOrderCanceled(order.getUsername(), order.getId(), cancelReason);
        return "redirect:/admin/orders?canceled";
    }

    @GetMapping("/admin/chat")
    public String chat(Model model) {
        model.addAttribute("activePage", "chat");
        return "admin/chat";
    }
}
