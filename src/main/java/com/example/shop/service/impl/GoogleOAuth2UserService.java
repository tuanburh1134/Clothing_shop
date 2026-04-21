package com.example.shop.service.impl;

import com.example.shop.entity.AppUser;
import com.example.shop.entity.Role;
import com.example.shop.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AppUserRepository appUserRepository;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public GoogleOAuth2UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = new DefaultOAuth2UserService().loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equalsIgnoreCase(registrationId)) {
            return oauthUser;
        }

        Map<String, Object> attributes = oauthUser.getAttributes();
        String email = getAsString(attributes, "email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account does not provide email");
        }

        String fullName = getAsString(attributes, "name");
        String avatarUrl = getAsString(attributes, "picture");

        AppUser user = appUserRepository.findByUsername(email)
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setUsername(email);
                    newUser.setPassword(ENCODER.encode(UUID.randomUUID().toString()));
                    newUser.setRole(Role.USER);
                    return newUser;
                });

        user.setContactEmail(email);
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }

        AppUser savedUser = appUserRepository.save(user);

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        if (nameAttributeKey == null || nameAttributeKey.isBlank()) {
            nameAttributeKey = "sub";
        }

        return new DefaultOAuth2User(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().name())),
                attributes,
                nameAttributeKey
        );
    }

    private String getAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
