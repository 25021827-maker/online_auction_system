package model;

public class Bidder extends User {

    private double accountBalance;

    public Bidder(String id, String username, String password, double initialBalance) {
        super(id, username, password, "BIDDER"); // Gọi constructor của lớp cha
        this.accountBalance = initialBalance;
    }

    public double getAccountBalance() { return accountBalance; }
    public void deductBalance(double amount) { this.accountBalance -= amount; }

    @Override
    public void displayRoleInfo() {
        System.out.println("[BIDDER] Người tham gia đấu giá: " + username + " - Số dư: $" + accountBalance);
    }
}