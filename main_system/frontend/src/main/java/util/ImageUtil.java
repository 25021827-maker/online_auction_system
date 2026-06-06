package util;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public final class ImageUtil {
    private ImageUtil() {}

    public static Image loadImage(String imageBase64, String imagePath, boolean backgroundLoading) {
        if (hasText(imageBase64)) {
            try {
                String normalized = stripDataUriPrefix(imageBase64.trim());
                byte[] bytes = Base64.getDecoder().decode(normalized);
                return new Image(new ByteArrayInputStream(bytes));
            } catch (Exception ignored) {
                return null;
            }
        }

        if (hasText(imagePath)) {
            try {
                return new Image(imagePath, backgroundLoading);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    public static String imageKey(String imageBase64, String imagePath) {
        if (hasText(imageBase64)) {
            return "base64:" + imageBase64.hashCode();
        }
        return "path:" + (imagePath == null ? "" : imagePath);
    }

    private static String stripDataUriPrefix(String value) {
        int commaIndex = value.indexOf(',');
        if (value.startsWith("data:image/") && commaIndex >= 0) {
            return value.substring(commaIndex + 1);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
