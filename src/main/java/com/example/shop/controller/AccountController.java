package com.example.shop.controller;

import com.example.shop.dto.AccountProfileForm;
import com.example.shop.entity.AppUser;
import com.example.shop.entity.CustomerOrder;
import com.example.shop.entity.OrderStatus;
import com.example.shop.exception.BadRequestException;
import com.example.shop.service.AccountService;
import com.example.shop.service.NotificationService;
import com.example.shop.service.OrderService;
import com.example.shop.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AccountController {

    private final AccountService accountService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final ReviewService reviewService;

    public AccountController(AccountService accountService,
                             OrderService orderService,
                             NotificationService notificationService,
                             ReviewService reviewService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.reviewService = reviewService;
    }

    @GetMapping("/account")
    public String accountProfile(@RequestParam(name = "edit", defaultValue = "false") boolean edit,
                                 Authentication authentication,
                                 Model model) {
        AppUser user = accountService.getByUsername(authentication.getName());

        if (!model.containsAttribute("accountProfileForm")) {
            AccountProfileForm form = new AccountProfileForm();
            form.setFullName(user.getFullName());
            form.setPhoneNumber(user.getPhoneNumber());
            form.setAddress(user.getAddress());
            form.setContactEmail(user.getContactEmail());
            model.addAttribute("accountProfileForm", form);
        }

        model.addAttribute("accountUser", user);
        model.addAttribute("activeTab", "profile");
        model.addAttribute("editMode", edit);
        return "account/profile";
    }

    @PostMapping("/account/profile")
    public String updateProfile(@Valid @ModelAttribute("accountProfileForm") AccountProfileForm accountProfileForm,
                                BindingResult bindingResult,
                                @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
                                Authentication authentication,
                                Model model) {
        AppUser user = accountService.getByUsername(authentication.getName());

        if (bindingResult.hasErrors()) {
            model.addAttribute("accountUser", user);
            model.addAttribute("activeTab", "profile");
            model.addAttribute("editMode", true);
            return "account/profile";
        }

        try {
            accountService.updateProfile(authentication.getName(), accountProfileForm, avatarFile);
        } catch (BadRequestException ex) {
            model.addAttribute("accountUser", user);
            model.addAttribute("activeTab", "profile");
            model.addAttribute("editMode", true);
            model.addAttribute("formError", ex.getMessage());
            return "account/profile";
        }

        return "redirect:/account?saved";
    }

    @GetMapping("/account/orders")
    public String accountOrders(Authentication authentication, Model model) {
        String username = authentication.getName();
        AppUser user = accountService.getByUsername(username);
        List<CustomerOrder> orders = orderService.getOrdersForUser(username);

        List<CustomerOrder> pendingOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .toList();
        List<CustomerOrder> approvedOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.APPROVED)
                .toList();
        List<CustomerOrder> deliveredOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .toList();
        List<CustomerOrder> canceledOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELED)
                .toList();

        model.addAttribute("accountUser", user);
        model.addAttribute("orders", orders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("approvedOrders", approvedOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("canceledOrders", canceledOrders);
        model.addAttribute("reviewedOrderItemIds", reviewService.getReviewedOrderItemIds(username));
        model.addAttribute("notifications", notificationService.getUserNotifications(username));
        model.addAttribute("activeTab", "orders");
        return "account/orders";
    }

    @PostMapping("/account/orders/{id}/cancel")
    public String cancelOrderByUser(@PathVariable Long id,
                                    @RequestParam("cancelReason") String cancelReason,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrderByUser(authentication.getName(), id, cancelReason);
            redirectAttributes.addFlashAttribute("orderActionSuccess", "Đã hủy đơn hàng thành công.");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("orderActionError", ex.getMessage());
        }

        return "redirect:/account/orders";
    }

    @PostMapping("/account/orders/{id}/received")
    public String markDeliveredByUser(@PathVariable Long id,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            orderService.markOrderDeliveredByUser(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("orderActionSuccess", "Đã xác nhận nhận hàng. Đơn đã chuyển sang mục Đã giao.");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("orderActionError", ex.getMessage());
        }

        return "redirect:/account/orders";
    }

    @PostMapping("/account/orders/{orderId}/review")
    public String submitOrderReview(@PathVariable Long orderId,
                                    @RequestParam("orderItemId") Long orderItemId,
                                    @RequestParam("productId") Long productId,
                                    @RequestParam("rating") Integer rating,
                                    @RequestParam("comment") String comment,
                                    @RequestParam(name = "reviewImages", required = false) MultipartFile[] reviewImages,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            reviewService.createReview(
                    authentication.getName(),
                    orderId,
                    orderItemId,
                    productId,
                    rating,
                    comment,
                    reviewImages
            );
            redirectAttributes.addFlashAttribute("orderActionSuccess", "Đã gửi đánh giá sản phẩm thành công.");
        } catch (BadRequestException ex) {
            redirectAttributes.addFlashAttribute("orderActionError", ex.getMessage());
        }

        return "redirect:/account/orders";
    }
}