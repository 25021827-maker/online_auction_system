package model;

public abstract class User {

    protected String id;
    protected String username;
    protected String password;
    protected String role;

    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public abstract void displayRoleInfo();

    // Tính Đóng gói (Encapsulation) - Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    public void setPassword(String password) { this.password = password; }
    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}