package FakeDB;

import Model.User;
import Model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FakeDB {

    // =========================
    // USER
    // =========================
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

    public static User getUser(String u, String p) {
        for (User user : users) {
            if (user.username.equals(u) && user.password.equals(p)) {
                return user;
            }
        }
        return null;
    }

    public static User getUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public static boolean exists(String u) {
        for (User user : users) {
            if (user.username.equals(u)) {
                return true;
            }
        }
        return false;
    }

    // =========================
    // PRODUCT
    // =========================
    public static List<Product> products = new ArrayList<>();
    private static final Random random = new Random();

    public static void addProduct(Product p) {
        products.add(p);
    }

    // =========================
    // GET ALL PRODUCTS
    // =========================
    public static List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    // =========================
    // FILTER STATUS
    // =========================
    public static List<Product> getByStatus(String status) {
        return products.stream()
                .filter(p -> p.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    // =========================
    // GET PRODUCTS BY SELLER
    // =========================
    public static List<Product> getProductsBySeller(String seller) {
        return products.stream()
                .filter(p -> p.getSeller().equals(seller))
                .collect(Collectors.toList());
    }

    // =========================
    // REMOVE / DELETE PRODUCT
    // =========================
    public static void removeProduct(Product product) {
        products.remove(product);
    }

    /**
     * 🎯 MỚI BỔ SUNG: Xóa sản phẩm theo ID (Dùng cho chức năng của Seller)
     */
    public static boolean deleteProductById(int id) {
        return products.removeIf(p -> p.getId() == id);
    }

    /**
     * 🎯 MỚI BỔ SUNG: Cập nhật sửa thông tin sản phẩm (Dùng cho chức năng của Seller)
     */
    public static boolean updateProduct(Product updatedProduct) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId() == updatedProduct.getId()) {
                products.set(i, updatedProduct);
                return true;
            }
        }
        return false;
    }

    /**
     * 🎯 MỚI BỔ SUNG: Hàm giả lập "bơm" giá Realtime tự động
     * Mỗi lần hàm này được gọi, hệ thống sẽ ngẫu nhiên tăng giá của một sản phẩm đang OPEN.
     */
    public static void simulateRealtimeBids() {
        // Lấy danh sách các sản phẩm đang trong phiên đấu giá
        List<Product> openProducts = products.stream()
                .filter(p -> p.getStatus().equals("OPEN"))
                .collect(Collectors.toList());

        if (openProducts.isEmpty()) return;

        // Tỷ lệ 30% có người trả giá ở mỗi giây để tránh giá tăng quá chóng mặt
        if (random.nextDouble() < 0.3) {
            // Chọn ngẫu nhiên 1 sản phẩm đang đấu giá
            Product targetProduct = openProducts.get(random.nextInt(openProducts.size()));

            // Tăng ngẫu nhiên một khoảng từ 10,000 VND đến 50,000 VND
            double raiseAmount = 10000 + (random.nextInt(9) * 5000);
            double newPrice = targetProduct.getCurrentPrice() + raiseAmount;

            targetProduct.setCurrentPrice(newPrice);
            targetProduct.setHighestBidder("bot_bidder_" + (random.nextInt(5) + 1));

            System.out.println("[FakeDB Realtime] San pham ID #" + targetProduct.getId() + " vua duoc bot tra gia len: " + newPrice + " VND");
        }
    }
}