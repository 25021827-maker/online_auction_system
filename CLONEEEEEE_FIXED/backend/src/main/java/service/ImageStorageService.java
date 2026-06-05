package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class ImageStorageService {

    private static final String PRODUCT_UPLOAD_DIR = "uploads/products";

    public String saveProductImage(String imageBase64, String originalFileName) throws IOException {
        if (imageBase64 == null || imageBase64.isBlank()) {
            return null;
        }

        Files.createDirectories(Paths.get(PRODUCT_UPLOAD_DIR));

        String extension = getSafeExtension(originalFileName);
        String fileName = UUID.randomUUID() + extension;

        Path targetPath = Paths.get(PRODUCT_UPLOAD_DIR, fileName);

        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        Files.write(targetPath, imageBytes);

        return PRODUCT_UPLOAD_DIR + "/" + fileName;
    }

    public String loadImageAsBase64(String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) {
                return null;
            }

            Path path = Paths.get(imagePath);

            if (!Files.exists(path)) {
                return null;
            }

            byte[] bytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getSafeExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return ".png";
        }

        String lower = originalFileName.toLowerCase();

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return ".jpg";
        }

        if (lower.endsWith(".png")) {
            return ".png";
        }

        if (lower.endsWith(".gif")) {
            return ".gif";
        }

        return ".png";
    }
}