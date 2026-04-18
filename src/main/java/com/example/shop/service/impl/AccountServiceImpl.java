package com.example.shop.service.impl;

import com.example.shop.dto.AccountProfileForm;
import com.example.shop.entity.AppUser;
import com.example.shop.exception.BadRequestException;
import com.example.shop.repository.AppUserRepository;
import com.example.shop.service.AccountService;
import com.example.shop.util.ImageStorageUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountServiceImpl implements AccountService {

    private final AppUserRepository appUserRepository;
    private final ImageStorageUtil imageStorageUtil;

    public AccountServiceImpl(AppUserRepository appUserRepository, ImageStorageUtil imageStorageUtil) {
        this.appUserRepository = appUserRepository;
        this.imageStorageUtil = imageStorageUtil;
    }

    @Override
    public AppUser getByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    @Override
    public void updateProfile(String username, AccountProfileForm form, MultipartFile avatarFile) {
        AppUser user = getByUsername(username);

        user.setFullName(trimToNull(form.getFullName()));
        user.setPhoneNumber(trimToNull(form.getPhoneNumber()));
        user.setAddress(trimToNull(form.getAddress()));
        user.setContactEmail(trimToNull(form.getContactEmail()));

        String avatarUrl = imageStorageUtil.storeAvatarImage(avatarFile);
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        appUserRepository.save(user);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}