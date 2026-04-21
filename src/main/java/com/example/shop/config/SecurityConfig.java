package com.example.shop.config;

import com.example.shop.service.impl.CustomUserDetailsService;
import com.example.shop.service.impl.GoogleOidcUserService;
import com.example.shop.service.impl.GoogleOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final GoogleOidcUserService googleOidcUserService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          GoogleOAuth2UserService googleOAuth2UserService,
                          GoogleOidcUserService googleOidcUserService) {
        this.customUserDetailsService = customUserDetailsService;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.googleOidcUserService = googleOidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/favicon.ico", "/css/**", "/js/**", "/images/**", "/auth", "/register", "/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/", "/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                    .requestMatchers("/cart/**", "/checkout/**", "/account/**").authenticated()
                    .requestMatchers("/products/*/buy-now", "/products/*/cart").authenticated()
                    .requestMatchers("/chat/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/chat/api/user/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/chat/api/**"))
                .formLogin(form -> form
                        .loginPage("/auth")
                        .loginProcessingUrl("/login")
                    .successHandler(buildSuccessHandler())
                        .failureUrl("/auth?error")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                    .loginPage("/auth")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(googleOAuth2UserService)
                            .oidcUserService(googleOidcUserService)
                    )
                    .successHandler(buildSuccessHandler())
                    .failureUrl("/auth?error")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth?logout")
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AuthenticationSuccessHandler buildSuccessHandler() {
        return (request, response, authentication) -> {
            RequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);

            if (savedRequest != null) {
                String targetUrl = savedRequest.getRedirectUrl();
                requestCache.removeRequest(request, response);
                response.sendRedirect(targetUrl);
                return;
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            response.sendRedirect(isAdmin ? "/admin/dashboard" : "/products");
        };
    }
}
