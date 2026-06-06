package Service.product;

import Model.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductFilterServiceTest {

    @Test
    void testFilterWithNullProducts() {
        // 1. Kiểm tra khi danh sách products truyền vào là null
        List<Product> result = ProductFilterService.filter(null, "iphone", "open", "Electronics", "New", 0.0, 1000.0, "newest");

        // Kết quả kỳ vọng: Hàm phải tự xử lý trả về danh sách rỗng (List.of()), không được văng lỗi crash app
        assertNotNull(result, "Kết quả trả về không được phép là null");
        assertTrue(result.isEmpty(), "Kết quả trả về phải là một danh sách rỗng");
    }

    @Test
    void testFilterWithEmptyProducts() {
        // 2. Kiểm tra khi danh sách products truyền vào trống rỗng (không có phần tử nào)
        List<Product> emptyList = new ArrayList<>();
        List<Product> result = ProductFilterService.filter(emptyList, "iphone", "open", "Electronics", "New", 0.0, 1000.0, "newest");

        // Kết quả kỳ vọng: Đi qua toàn bộ các bộ lọc và trả về danh sách trống an toàn
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}