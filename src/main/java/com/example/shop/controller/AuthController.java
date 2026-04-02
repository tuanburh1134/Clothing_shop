package com.example.shop.controller;

import com.example.shop.dto.RegisterRequest;
import com.example.shop.exception.BadRequestException;
import com.example.shop.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({"/", "/auth"})
    public String authPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/auth";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeTab", "register");
            return "auth/auth";
        }

        try {
            authService.register(registerRequest);
        } catch (BadRequestException ex) {
            model.addAttribute("activeTab", "register");
            model.addAttribute("registerError", ex.getMessage());
            return "auth/auth";
        }

        return "redirect:/auth?registered";
    }
}
