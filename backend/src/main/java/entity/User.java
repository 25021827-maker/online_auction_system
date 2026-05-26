package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User {
    private Long userId;
    private String username;
    private String password; // đã hash
    private String email;
    private String fullName;
    private boolean isBidder;
    private boolean isSeller;
    private boolean isAdmin;
    private LocalDateTime createdAt;
    private BigDecimal balance; // thêm cột balance nếu chưa có

    // Constructors, getters, setters
    public User() {}
    // ... tất cả getter/setter
}