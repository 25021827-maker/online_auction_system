package Model;

public class User {
    public String username;
    public String password;

    // ===== THÔNG TIN MỞ RỘNG =====
    private String email;
    private String phone;
    private String nationality;
    private String address;

    // ===== WALLET =====
    private double balance;

    // ===== CONSTRUCTOR CŨ (GIỮ LẠI) =====
    public User(String u, String p) {
        this.username = u;
        this.password = p;
        this.balance = 0;
    }

    // ===== CONSTRUCTOR ĐẦY ĐỦ =====
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