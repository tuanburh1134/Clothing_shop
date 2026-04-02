package com.example.shop.config;

import com.example.shop.util.ImageStorageUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    private final ImageStorageUtil imageStorageUtil;

    public WebResourceConfig(ImageStorageUtil imageStorageUtil) {
        this.imageStorageUtil = imageStorageUtil;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + imageStorageUtil.getProductImageDirectoryAbsolutePath() + "/";
        registry.addResourceHandler("/images/products/**")
                .addResourceLocations(location, "classpath:/static/images/products/");
    }
}
