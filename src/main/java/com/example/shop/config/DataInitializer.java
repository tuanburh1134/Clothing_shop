package com.example.shop.config;

import com.example.shop.entity.AppUser;
import com.example.shop.entity.Role;
import com.example.shop.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initAdminUser(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin.username}") String adminUsername,
            @Value("${app.security.admin.password}") String adminPassword
    ) {
        return args -> {
            if (appUserRepository.existsByUsername(adminUsername)) {
                return;
            }

            AppUser admin = new AppUser(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN
            );

            appUserRepository.save(admin);
            log.info("Admin account initialized: {}", adminUsername);
        };
    }
}
