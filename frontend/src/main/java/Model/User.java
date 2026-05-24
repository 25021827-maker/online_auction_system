package Model;

public class User {
    public String username;
    public String password;

    // ===== THÔNG TIN MỞ RỘNG =====
    private String email;
    private String phone;
    private String nationality;
    private String address;

    // ===== AVATAR PATH (MỚI BỔ SUNG) =====
    private String avatarPath; // Lưu đường dẫn file ảnh cá nhân của người dùng

    // ===== WALLET =====
    private double balance;

    // ===== CONSTRUCTOR CŨ (ĐÃ CẬP NHẬT AVATAR) =====
    public User(String u, String p) {
        this.username = u;
        this.password = p;
        this.balance = 0;
        this.avatarPath = ""; // Mặc định trống để dùng ảnh default
    }

    // ===== CONSTRUCTOR ĐẦY ĐỦ (ĐÃ CẬP NHẬT AVATAR) =====
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
    }

    // ===== GETTER & SETTER CHO AVATAR (MỚI BỔ SUNG) =====
    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        // Khi user upload ảnh mới ở trang Profile, ta sẽ gọi hàm này để cập nhật đường dẫn
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