package com.example.shop.controller;

import com.example.shop.dto.AccountProfileForm;
import com.example.shop.entity.AppUser;
import com.example.shop.exception.BadRequestException;
import com.example.shop.service.AccountService;
import com.example.shop.service.NotificationService;
import com.example.shop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AccountController {

    private final AccountService accountService;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public AccountController(AccountService accountService,
                             OrderService orderService,
                             NotificationService notificationService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.notificationService = notificationService;
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
        AppUser user = accountService.getByUsername(authentication.getName());
        model.addAttribute("accountUser", user);
        model.addAttribute("orders", orderService.getOrdersForUser(authentication.getName()));
        model.addAttribute("notifications", notificationService.getUserNotifications(authentication.getName()));
        model.addAttribute("activeTab", "orders");
        return "account/orders";
    }
}