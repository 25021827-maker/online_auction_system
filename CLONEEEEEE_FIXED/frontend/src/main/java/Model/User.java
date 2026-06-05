package Model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private double balance;
    private double availableBalance;

    // Các trường phục vụ riêng cho UI Frontend
    private String avatarPath;
    private List<Integer> watchlistProductIds;

    // Constructor mặc định cho Gson tạo object
    public User() {
        this.watchlistProductIds = new ArrayList<>();
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public double getBalance() { return balance; }
    public double getAvailableBalance() { return availableBalance; }
    public String getAvatarPath() { return avatarPath; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(String role) { this.role = role; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    // =======================================================
    // CÁC HÀM TIỆN ÍCH DÀNH RIÊNG CHO GIAO DIỆN (UI)
    // =======================================================

    // Dùng khi người dùng bấm đặt giá (Tạm trừ tiền trên UI để hiện ngay lập tức)
    public void deductMoney(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
        }
    }

    // Dùng cho WatchlistController (Lấy danh sách ID đang theo dõi)
    // =======================================================
    // CÁC HÀM WATCHLIST (Đã sửa tên khớp 100% với ProductCard)
    // =======================================================

    // Dành cho WatchlistController lấy toàn bộ ID
    public List<Integer> getWatchlistProductIds() {
        if (watchlistProductIds == null) {
            watchlistProductIds = new ArrayList<>();
        }
        return watchlistProductIds;
    }

    // 1. Hàm kiểm tra xem có đang theo dõi không (Sửa lỗi dòng 98, 193)
    public boolean isWatching(int productId) {
        if (watchlistProductIds == null) return false;
        return watchlistProductIds.contains(productId);
    }

    // 2. Hàm thêm vào danh sách theo dõi (Sửa lỗi dòng 103)
    public void addToWatchlist(int productId) {
        if (watchlistProductIds == null) {
            watchlistProductIds = new ArrayList<>();
        }
        if (!watchlistProductIds.contains(productId)) {
            watchlistProductIds.add(productId);
        }
    }

    // 3. Hàm xóa khỏi danh sách theo dõi (Sửa lỗi dòng 100)
    public void removeFromWatchlist(int productId) {
        if (watchlistProductIds != null) {
            // Ép kiểu Integer.valueOf để Java không nhầm sang việc xóa theo số thứ tự (index)
            watchlistProductIds.remove(Integer.valueOf(productId));
        }
    }
    public void setWatchlistProductIds(List<Integer> ids) {
        if (ids == null) {
            this.watchlistProductIds = new ArrayList<>();
        } else {
            this.watchlistProductIds = ids;
        }
    }
}
