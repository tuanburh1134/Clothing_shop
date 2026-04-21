package com.example.shop.service.impl;

import com.example.shop.entity.AppUser;
import com.example.shop.entity.Role;
import com.example.shop.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleOidcUserService extends OidcUserService {

    private final AppUserRepository appUserRepository;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public GoogleOidcUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equalsIgnoreCase(registrationId)) {
            return oidcUser;
        }

        Map<String, Object> attributes = oidcUser.getAttributes();
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

        String nameAttributeKey = attributes.containsKey("email") ? "email" : "sub";

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().name())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                nameAttributeKey
        );
    }

    private String getAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
