package com.example.shop.service;

import com.example.shop.dto.AccountProfileForm;
import com.example.shop.entity.AppUser;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {
    AppUser getByUsername(String username);

    void updateProfile(String username, AccountProfileForm form, MultipartFile avatarFile);
}