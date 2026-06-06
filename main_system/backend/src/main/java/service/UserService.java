package service;

import dao.UserDAO;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    // Đổi boolean thành model.User
    public model.User login(String username, String password) {
        return userDAO.authenticateUser(username, password);
    }
    // Hàm này gọi trực tiếp UserDAO đã viết ở bài trước
    public boolean register(String username, String password, String email, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return false;
        }
        Long newUserId = userDAO.registerUser(username, password, email, role);
        return newUserId != null; // Trả về true nếu CSDL cấp phát ID thành công
    }
}
