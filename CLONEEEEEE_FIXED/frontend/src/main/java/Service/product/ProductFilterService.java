package Service.product;

import Model.Product;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
        if (products == null) {
            return List.of();
        }

        List<Product> filtered = products;

        // =====================================================
        // SEARCH: tim theo ten, mo ta, seller, category, condition hoac ID
        // =====================================================
        if (keyword != null && !keyword.trim().isEmpty()) {
            String query = normalize(keyword);

            filtered = filtered.stream()
                    .filter(p -> contains(p.getTitle(), query)
                            || contains(p.getDescription(), query)
                            || contains(p.getSeller(), query)
                            || contains(p.getCategory(), query)
                            || contains(p.getCondition(), query)
                            || String.valueOf(p.getId()).equals(query)
                            || ("#" + p.getId()).equals(query))
                    .collect(Collectors.toList());
        }

        // =====================================================
        // STATUS FILTER
        // Chi loc status that: OPEN, RUNNING, SCHEDULED, FINISHED...
        // Khong xu ly Newest / Ending Soon o day.
        // =====================================================
        if (isRealFilter(status)) {
            String normalizedStatus = normalize(status);

            filtered = filtered.stream()
                    .filter(p -> normalize(p.getStatus()).equals(normalizedStatus))
                    .collect(Collectors.toList());
        }

        // =====================================================
        // CATEGORY FILTER
        // =====================================================
        if (isRealFilter(category)) {
            String normalizedCategory = normalize(category);

            filtered = filtered.stream()
                    .filter(p -> normalize(p.getCategory()).equals(normalizedCategory))
                    .collect(Collectors.toList());
        }

        // =====================================================
        // CONDITION FILTER
        // =====================================================
        if (isRealFilter(condition)) {
            String normalizedCondition = normalize(condition);

            filtered = filtered.stream()
                    .filter(p -> normalize(p.getCondition()).equals(normalizedCondition))
                    .collect(Collectors.toList());
        }

        // =====================================================
        // PRICE FILTER
        // Neu min > max thi doi cho de khong tra ve rong vo ly
        // =====================================================
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            double temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        if (minPrice != null) {
            Double finalMinPrice = minPrice;
            filtered = filtered.stream()
                    .filter(p -> p.getCurrentPrice() >= finalMinPrice)
                    .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            Double finalMaxPrice = maxPrice;
            filtered = filtered.stream()
                    .filter(p -> p.getCurrentPrice() <= finalMaxPrice)
                    .collect(Collectors.toList());
        }

        // =====================================================
        // SORT
        // =====================================================
        if (isRealFilter(sortType)) {
            switch (sortType) {
                case "Price: Low to High":
                    filtered = filtered.stream()
                            .sorted(Comparator.comparingDouble(Product::getCurrentPrice))
                            .collect(Collectors.toList());
                    break;

                case "Price: High to Low":
                    filtered = filtered.stream()
                            .sorted(Comparator.comparingDouble(Product::getCurrentPrice).reversed())
                            .collect(Collectors.toList());
                    break;

                case "Newest":
                case "Newest First":
                    /*
                     * Tam thoi sort theo Product.createdAt.
                     * Luu y: createdAt hien la gio frontend tao object,
                     * chua phai created_at that tu database.
                     */
                    filtered = filtered.stream()
                            .sorted(Comparator.comparing(
                                    Product::getCreatedAt,
                                    Comparator.nullsLast(Comparator.reverseOrder())
                            ))
                            .collect(Collectors.toList());
                    break;

                case "Ending Soon":
                    /*
                     * Sap het han: chi nen sap xep cac phien dang mo/chay,
                     * dua endTime gan nhat len truoc.
                     */
                    filtered = filtered.stream()
                            .filter(ProductFilterService::isOpenOrRunning)
                            .sorted(Comparator.comparing(
                                    Product::getEndTime,
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            ))
                            .collect(Collectors.toList());
                    break;

                default:
                    break;
            }
        }

        return filtered;
    }

    private static boolean isOpenOrRunning(Product p) {
        if (p == null) {
            return false;
        }

        String status = normalize(p.getStatus());
        return status.equals("open") || status.equals("running");
    }

    private static boolean contains(String value, String query) {
        return normalize(value).contains(query);
    }

    private static boolean isRealFilter(String value) {
        if (value == null) {
            return false;
        }

        String v = value.trim();

        return !v.isEmpty()
                && !"All".equalsIgnoreCase(v)
                && !"STATUS".equalsIgnoreCase(v)
                && !"CATEGORY".equalsIgnoreCase(v)
                && !"CONDITION".equalsIgnoreCase(v)
                && !"SORT".equalsIgnoreCase(v);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}