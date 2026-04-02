package com.example.shop.service.impl;

import com.example.shop.dto.RegisterRequest;
import com.example.shop.entity.AppUser;
import com.example.shop.entity.Role;
import com.example.shop.exception.BadRequestException;
import com.example.shop.repository.AppUserRepository;
import com.example.shop.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Confirm password does not match");
        }

        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        AppUser user = new AppUser(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                Role.USER
        );

        appUserRepository.save(user);
    }
}
