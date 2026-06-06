package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class ImageStorageServiceTest {

    private final ImageStorageService imageStorageService = new ImageStorageService();

    @Test
    public void testSaveProductImage_NullOrBlank_ReturnsNull() throws Exception {
        // Nếu truyền chuỗi rỗng hoặc null, phải trả về null và không làm gì cả
        assertNull(imageStorageService.saveProductImage(null, "test.png"));
        assertNull(imageStorageService.saveProductImage("", "test.png"));
        assertNull(imageStorageService.saveProductImage("   ", "test.png"));
    }

    @Test
    public void testSaveProductImage_Success() throws Exception {
        // Tạo 1 chuỗi Base64 giả
        String dummyBase64 = Base64.getEncoder().encodeToString("dummy_image_data".getBytes());

        // Bắt đầu LÀM GIẢ hệ thống File của máy tính
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Ra lệnh: Nếu ai đó gọi hàm tạo thư mục hay ghi file, cứ vờ như đã làm xong (thenReturn null)
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            mockedFiles.when(() -> Files.write(any(Path.class), any(byte[].class))).thenReturn(null);

            // Chạy hàm cần test
            String savedPath = imageStorageService.saveProductImage(dummyBase64, "avatar.jpg");

            // Kiểm tra kết quả trả về
            assertNotNull(savedPath);
            assertTrue(savedPath.startsWith("uploads/products/"));
            assertTrue(savedPath.endsWith(".jpg")); // Phải lấy đúng đuôi file

            // Đảm bảo lệnh Ghi File đã thực sự được gọi
            mockedFiles.verify(() -> Files.write(any(Path.class), any(byte[].class)));
        }
    }

    @Test
    public void testLoadImageAsBase64_FileNotFound_ReturnsNull() {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Giả lập file không tồn tại trên ổ cứng
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            String result = imageStorageService.loadImageAsBase64("dummy_path.png");

            assertNull(result, "Hàm phải trả về null nếu file không tồn tại");
        }
    }
}