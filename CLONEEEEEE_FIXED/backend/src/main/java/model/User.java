package model;

public abstract class User {
    protected Long id;
    protected String username;
    protected transient String password;
    protected String email;     // [BỔ SUNG]
    protected String fullName;  // [BỔ SUNG]
    protected String role;
    protected double balance;

    public User(Long id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.balance = 0.0;
    }

    public abstract void displayRoleInfo();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public String getRole() { return role; }
    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public void setPassword(String password) { this.password = password; }

    /**
     * @deprecated Password verification is handled by dao.UserDAO with BCrypt.
     */
    @Deprecated
    public boolean checkPassword(String inputPassword) {
        return this.password != null && this.password.equals(inputPassword);
    }
}
