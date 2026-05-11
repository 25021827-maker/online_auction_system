package Model;

public class User {
    public String username;
    public String password;

    public User(String u, String p) {
        this.username = u;
        this.password = p;
    }

    // Getter và Setter (Bắt buộc phải có để TableView hiển thị được)
    public String getUsername() { return username; }
    public String getPassword() { return password;}
}