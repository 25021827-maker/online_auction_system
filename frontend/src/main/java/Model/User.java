package Model;

public class User {
    private String username;
    private String email;
    private String role; // "ADMIN" hoặc "USER"

    public User(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    // Getter và Setter (Bắt buộc phải có để TableView hiển thị được)
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}