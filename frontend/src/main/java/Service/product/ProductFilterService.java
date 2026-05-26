package Service.product;

import Model.Product;

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

        // SEARCH
        if (keyword != null && !keyword.isEmpty()) {
            filtered = filtered.stream()
                    .filter(p -> p.getTitle().toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // STATUS
        if (status != null && !status.equals("All")) {
            filtered = filtered.stream()
                    .filter(p -> p.getStatus().equals(status))
                    .collect(Collectors.toList());
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

                case "Giá thấp → cao":
                    filtered = filtered.stream()
                            .sorted((a, b) ->
                                    Double.compare(a.getCurrentPrice(), b.getCurrentPrice()))
                            .collect(Collectors.toList());
                    break;

                case "Giá cao → thấp":
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


