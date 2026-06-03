package Session;

import Model.User;

public class Session {
    // Lưu trữ thông tin người dùng đang đăng nhập
    public static User currentUser;

    // Kiểm tra xem đã đăng nhập chưa
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    // Xóa phiên làm việc khi Đăng xuất
    public static void clear() {
        currentUser = null;
    }
}