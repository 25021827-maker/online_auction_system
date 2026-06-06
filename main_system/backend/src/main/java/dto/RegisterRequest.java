package dto;

public class RegisterRequest {
    public String username;
    public String password;
    public String email;
    public String role; // Chỉ nhận: "BIDDER" hoặc "SELLER"
}