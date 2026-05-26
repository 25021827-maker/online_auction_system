package Model;

public class User {
    public String id;
    public String username;
    public String password;
    public String role;
    public double balance;

    // Constructor cũ (dùng cho FakeDB)
    public User(String u, String p) {
        this.username = u;
        this.password = p;
        this.role = "BIDDER";
        this.balance = 0;
    }

    // Constructor mới (từ server)
    public User(String id, String username, String password, String role, double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.balance = balance;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }

    public void addMoney(double amount) { this.balance += amount; }
    public boolean deductMoney(double amount) {
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }
}