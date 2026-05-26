package Session;

import Model.User;

public class UserSession {
    private static UserSession instance;
    private User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public User getCurrentUser() { return currentUser; }
    public String getUsername() { return currentUser != null ? currentUser.username : null; }
    public double getBalance() { return currentUser != null ? currentUser.balance : 0; }
    public void setBalance(double balance) { if (currentUser != null) currentUser.balance = balance; }
    public Long getUserId() {
        if (currentUser != null && currentUser.id != null) {
            try { return Long.parseLong(currentUser.id); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}