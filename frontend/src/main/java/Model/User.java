package Model;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String username;
    public String password;

    // ===== THÔNG TIN MỞ RỘNG =====
    private String email;
    private String phone;
    private String nationality;
    private String address;

    // ===== AVATAR PATH =====
    private String avatarPath; // Lưu đường dẫn file ảnh cá nhân của người dùng

    // ===== WALLET =====
    private double balance;

    // =====================================================
    // 🎯 MỚI BỔ SUNG: DANH SÁCH LƯU ID CÁC SẢN PHẨM THEO DÕI (WATCHLIST)
    // =====================================================
    private List<Integer> watchlistProductIds;

    // ===== CONSTRUCTOR CŨ (ĐÃ CẬP NHẬT WATCHLIST) =====
    public User(String u, String p) {
        this.username = u;
        this.password = p;
        this.balance = 0;
        this.avatarPath = ""; // Mặc định trống để dùng ảnh default
        this.watchlistProductIds = new ArrayList<>(); // Khởi tạo danh sách trống
    }

    // ===== CONSTRUCTOR ĐẦY ĐỦ (ĐÃ CẬP NHẬT WATCHLIST) =====
    public User(String username, String password,
                String email, String phone,
                String nationality, String address) {

        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.nationality = nationality;
        this.address = address;
        this.balance = 0;
        this.avatarPath = ""; // Mặc định trống để dùng ảnh default
        this.watchlistProductIds = new ArrayList<>(); // Khởi tạo danh sách trống
    }

    // =====================================================
    // 🎯 MỚI BỔ SUNG: CÁC TIỆN ÍCH XỬ LÝ WATCHLIST
    // =====================================================

    /**
     * Lấy ra toàn bộ danh sách ID sản phẩm đang theo dõi
     */
    public List<Integer> getWatchlistProductIds() {
        if (this.watchlistProductIds == null) {
            this.watchlistProductIds = new ArrayList<>();
        }
        return this.watchlistProductIds;
    }

    /**
     * Thêm một sản phẩm vào Watchlist (Nếu chưa tồn tại)
     */
    public void addToWatchlist(int productId) {
        if (!getWatchlistProductIds().contains(productId)) {
            this.watchlistProductIds.add(productId);
            System.out.println("[Watchlist] Da them san pham ID #" + productId + " vao danh sach theo doi.");
        }
    }

    /**
     * Xóa một sản phẩm khỏi Watchlist
     */
    public void removeFromWatchlist(int productId) {
        if (getWatchlistProductIds().contains(productId)) {
            this.watchlistProductIds.remove(Integer.valueOf(productId));
            System.out.println("[Watchlist] Da xoa san pham ID #" + productId + " khoi danh sach theo doi.");
        }
    }

    /**
     * Kiểm tra xem sản phẩm này đã được User theo dõi chưa để đổi màu nút bấm ngoài giao diện
     */
    public boolean isWatching(int productId) {
        return getWatchlistProductIds().contains(productId);
    }

    // ===== GETTER & SETTER CHO AVATAR =====
    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    // ===== GETTER =====
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNationality() { return nationality; }
    public String getAddress() { return address; }

    public double getBalance() { return balance; }

    // ===== WALLET LOGIC =====
    public void addMoney(double amount) {
        this.balance += amount;
    }

    public boolean deductMoney(double amount) {
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }
}