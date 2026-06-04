package Service.product;

import Model.Product;
import util.VietnamTime;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class ProductFilterService {

    public static List<Product> filter(
            List<Product> products,
            String keyword,
            String status,
            String category,
            String condition,
            Double minPrice,
            Double maxPrice,
            String sortType
    ) {

        List<Product> filtered = products;

        // =====================================================
        // 🎯 SEARCH GỘP: TÌM KIẾM THEO TÊN HOẶC MÃ SỐ ID
        // =====================================================
        if (keyword != null && !keyword.isEmpty()) {
            String query = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(query) // Khớp tên
                            || String.valueOf(p.getId()).equals(query))     // HOẶC khớp chính xác ID số
                    .collect(Collectors.toList());
        }

        // =====================================================
        // 🎯 STATUS: LỌC TRẠNG THÁI + BỘ LỌC THỜI GIAN NÂNG CAO
        // =====================================================
        if (status != null && !status.equals("All")) {
            LocalDateTime now = VietnamTime.now();

            switch (status) {
                case "Ending Soon":
                    // Sắp kết thúc: Sản phẩm phải đang OPEN và thời gian còn lại từ 0 đến 24 giờ
                    filtered = filtered.stream()
                            .filter(p -> ("OPEN".equals(p.getStatus()) || "RUNNING".equals(p.getStatus()))
                                    && p.getEndTime() != null
                                    && !Duration.between(now, p.getEndTime()).isNegative()
                                    && Duration.between(now, p.getEndTime()).toHours() <= 24)
                            .collect(Collectors.toList());
                    break;

                case "Newest":
                    // Mới đăng: Được tạo ra trong vòng 24 giờ qua
                    // 🌟 Giả định Model Product của bạn có phương thức getCreatedAt() trả về LocalDateTime
                    filtered = filtered.stream()
                            .filter(p -> p.getCreatedAt() != null
                                    && Duration.between(p.getCreatedAt(), now).toHours() <= 24)
                            .collect(Collectors.toList());
                    break;

                default:
                    // Các trạng thái mặc định như cũ (OPEN, SOLD, SCHEDULED...)
                    filtered = filtered.stream()
                            .filter(p -> p.getStatus().equals(status))
                            .collect(Collectors.toList());
                    break;
            }
        }

        // CATEGORY
        if (category != null && !category.equals("All")) {
            filtered = filtered.stream()
                    .filter(p -> p.getCategory() != null
                            && p.getCategory().equals(category))
                    .collect(Collectors.toList());
        }

        // CONDITION
        if (condition != null && !condition.equals("All")) {
            filtered = filtered.stream()
                    .filter(p -> p.getCondition() != null
                            && p.getCondition().equals(condition))
                    .collect(Collectors.toList());
        }

        // MIN PRICE
        if (minPrice != null) {
            filtered = filtered.stream()
                    .filter(p -> p.getCurrentPrice() >= minPrice)
                    .collect(Collectors.toList());
        }

        // MAX PRICE
        if (maxPrice != null) {
            filtered = filtered.stream()
                    .filter(p -> p.getCurrentPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // SORT
        if (sortType != null) {

            switch (sortType) {

                case "Price: Low to High":
                    filtered = filtered.stream()
                            .sorted((a, b) ->
                                    Double.compare(a.getCurrentPrice(), b.getCurrentPrice()))
                            .collect(Collectors.toList());
                    break;

                case "Price: High to Low":
                    filtered = filtered.stream()
                            .sorted((a, b) ->
                                    Double.compare(b.getCurrentPrice(), a.getCurrentPrice()))
                            .collect(Collectors.toList());
                    break;
            }
        }

        return filtered;
    }
}
