package util;

public class UserSession {
    private static UserSession instance;
    private Long userId;
    private String username;
    private double balance;
    private String role;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public void clear() {
        userId = null;
        username = null;
        balance = 0;
        role = null;
    }
}