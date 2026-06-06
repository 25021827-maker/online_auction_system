package model;

public class ItemFactory {

    public static Item createItem(String type, Long id, String name, String description, double startingPrice, Object extraParam) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = (extraParam != null) ? ((Number) extraParam).intValue() : 12;
                return new Electronics(id, name, description, startingPrice, warranty);

            case "ART":
                String artist = (extraParam != null) ? (String) extraParam : "Unknown";
                return new Art(id, name, description, startingPrice, artist);

            case "VEHICLE":
                double mileage = (extraParam instanceof Number) ? ((Number) extraParam).doubleValue() : 0.0;
                return new Vehicle(id, name, description, startingPrice, mileage);

            case "OTHER":
                return new OtherItem(id, name, description, startingPrice);

            default:
                return new OtherItem(id, name, description, startingPrice);
        }
    }
}