package com.example.shop.util;

import com.example.shop.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ImageStorageUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Path PRODUCT_IMAGE_DIR = Paths.get("uploads", "products");
    private static final Path AVATAR_IMAGE_DIR = Paths.get("uploads", "avatars");

    public String storeProductImage(MultipartFile file) {
        return storeImage(file, PRODUCT_IMAGE_DIR, "/images/products/", "Cannot save product image");
    }

    public String storeAvatarImage(MultipartFile file) {
        return storeImage(file, AVATAR_IMAGE_DIR, "/images/avatars/", "Cannot save avatar image");
    }

    private String storeImage(MultipartFile file,
                              Path targetDir,
                              String publicPrefix,
                              String saveErrorMessage) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Only image files .jpg, .jpeg, .png, .webp are allowed");
        }

        try {
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return publicPrefix + filename;
        } catch (IOException ex) {
            throw new BadRequestException(saveErrorMessage);
        }
    }

    public String storeProductImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return null;
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String imageUrl = storeProductImage(file);
            if (imageUrl != null) {
                imageUrls.add(imageUrl);
            }
        }

        return imageUrls.isEmpty() ? null : String.join("||", imageUrls);
    }

    public void deleteProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.startsWith("/images/products/")) {
            return;
        }

        try {
            String filename = imageUrl.substring("/images/products/".length());
            Path filePath = PRODUCT_IMAGE_DIR.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Ignore file delete errors to avoid breaking business operations.
        }
    }

    public void deleteProductImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        imageUrls.forEach(this::deleteProductImage);
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BadRequestException("Image file extension is required");
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    public String getProductImageDirectoryAbsolutePath() {
        return PRODUCT_IMAGE_DIR.toAbsolutePath().normalize().toString();
    }

    public String getAvatarImageDirectoryAbsolutePath() {
        return AVATAR_IMAGE_DIR.toAbsolutePath().normalize().toString();
    }
}
