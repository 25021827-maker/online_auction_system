package model;

public class ItemFactory {

    public static Item createItem(String type, String id, String name, String description, double startingPrice, Object extraParam) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = (extraParam != null) ? (Integer) extraParam : 12;
                return new Electronics(id, name, description, startingPrice, warranty);

            case "ART":
                String artist = (extraParam != null) ? (String) extraParam : "Unknown";
                return new Art(id, name, description, startingPrice, artist);

            case "VEHICLE":
                double mileage = (extraParam != null) ? (Double) extraParam : 0.0;
                return new Vehicle(id, name, description, startingPrice, mileage);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}