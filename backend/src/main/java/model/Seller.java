package model;

public class Seller extends User {
    private double shopRating;

    public Seller(String id, String username, String password) {
        super(id, username, password, "SELLER");
        this.shopRating = 5.0;
    }

    public double getShopRating() { return shopRating; }
    public void setShopRating(double shopRating) { this.shopRating = shopRating; }

    @Override
    public void displayRoleInfo() {
        System.out.println("[SELLER] Chủ cửa hàng/Người bán: " + username + " - Đánh giá: " + shopRating + " sao");
    }
}