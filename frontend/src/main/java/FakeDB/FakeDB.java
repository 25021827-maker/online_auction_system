package FakeDB;

import Model.User;
import Model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeDB {

    // ===== USER =====
    public static List<User> users = new ArrayList<>();

    static {
        users.add(new User("admin", "123"));
    }

    public static void addUser(String u, String p) {
        users.add(new User(u, p));
    }

    public static boolean checkLogin(String u, String p) {
        for (User user : users) {
            if (user.username.equals(u) && user.password.equals(p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean exists(String u) {
        for (User user : users) {
            if (user.username.equals(u)) return true;
        }
        return false;
    }

    // ===== PRODUCT =====
    public static List<Product> products = new ArrayList<>();

    // 👉 dữ liệu mẫu (rất quan trọng để test UI)
    static {
        products.add(new Product("iPhone 13", 1000, "10 phút", "", "OPEN", "admin"));
        products.add(new Product("Laptop Dell", 1500, "20 phút", "", "OPEN", "admin"));
        products.add(new Product("Tai nghe Sony", 200, "5 phút", "", "SOLD", "admin"));
    }

    public static void addProduct(Product p) {
        products.add(p);
    }

    // 👉 trả về bản copy (tránh sửa trực tiếp list)
    public static List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    // 👉 filter theo status (phục vụ tab)
    public static List<Product> getByStatus(String status) {
        return products.stream()
                .filter(p -> p.getStatus().equals(status))
                .collect(Collectors.toList());
    }
}
