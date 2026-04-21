package com.example.shop.controller;

import com.example.shop.entity.Product;
import com.example.shop.entity.OrderStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        public String dashboard(@RequestParam(name = "period", defaultValue = "month") String period,
                    Model model) {
        List<Product> products = productService.getAllProducts();
        List<com.example.shop.entity.CustomerOrder> allOrders = orderService.getAllOrdersForAdmin();

        String normalizedPeriod = normalizePeriod(period);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = resolvePeriodStart(normalizedPeriod, now);

        long soldUnits = allOrders.stream()
            .filter(this::isSoldOrder)
            .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(periodStart))
            .flatMap(order -> order.getItems().stream())
            .map(item -> item.getQuantity() == null ? 0 : item.getQuantity())
            .mapToLong(Integer::longValue)
            .sum();

        BigDecimal periodRevenue = allOrders.stream()
            .filter(this::isSoldOrder)
            .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(periodStart))
            .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = products.stream()
            .map(Product::getQuantity)
            .filter(quantity -> quantity != null)
            .mapToInt(Integer::intValue)
            .sum();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
        List<String> revenueLabels = new ArrayList<>();
        List<BigDecimal> revenueValues = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

            BigDecimal revenue = allOrders.stream()
                .filter(this::isSoldOrder)
                .filter(order -> order.getCreatedAt() != null
                    && !order.getCreatedAt().isBefore(start)
                    && order.getCreatedAt().isBefore(end))
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueLabels.add(ym.format(monthFormatter));
            revenueValues.add(revenue);
        }

        model.addAttribute("period", normalizedPeriod);
        model.addAttribute("periodLabel", resolvePeriodLabel(normalizedPeriod));
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("soldUnits", soldUnits);
        model.addAttribute("periodRevenue", periodRevenue);
        model.addAttribute("revenueLabels", revenueLabels);
        model.addAttribute("revenueValues", revenueValues);
        return "admin/dashboard";
    }

    @GetMapping("/admin/inventory")
    public String inventory(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "admin/inventory";
    }

    @GetMapping("/admin/revenue")
    public String revenue() {
        return "redirect:/admin/dashboard";
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

    private boolean isSoldOrder(com.example.shop.entity.CustomerOrder order) {
        return order.getStatus() == OrderStatus.APPROVED || order.getStatus() == OrderStatus.DELIVERED;
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return "month";
        }

        return switch (period.toLowerCase()) {
            case "day" -> "day";
            case "year" -> "year";
            default -> "month";
        };
    }

    private LocalDateTime resolvePeriodStart(String period, LocalDateTime now) {
        return switch (period) {
            case "day" -> now.toLocalDate().atStartOfDay();
            case "year" -> LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
            default -> YearMonth.from(now).atDay(1).atStartOfDay();
        };
    }

    private String resolvePeriodLabel(String period) {
        return switch (period) {
            case "day" -> "ngày";
            case "year" -> "năm";
            default -> "tháng";
        };
    }
}
