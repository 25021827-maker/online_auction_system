package service;

import dao.UserDAO;
import model.Bidder;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private User mockUser;

    @BeforeEach
    public void setUp() {
        // Chuẩn bị sẵn một User giả định để dùng cho các test case
        mockUser = new Bidder(1L, "testuser", "test@gmail.com", 1000.0);
        mockUser.setRole("BIDDER");
    }

    // ==========================================
    // TEST LOGIC ĐĂNG NHẬP (LOGIN)
    // ==========================================

    @Test
    public void testLogin_Success() {
        // Bao bọc lệnh chạy trong MockedConstruction để chặn mọi lệnh 'new UserDAO()'
        try (MockedConstruction<UserDAO> mockedDao = mockConstruction(UserDAO.class,
                (mock, context) -> {
                    // Định nghĩa kịch bản: Khi gọi authenticateUser đúng pass thì trả về mockUser
                    when(mock.authenticateUser("testuser", "123456")).thenReturn(mockUser);
                })) {

            // Khởi tạo UserService BÊN TRONG khối try để nó nhận được DAO giả
            UserService userService = new UserService();

            // Chạy hàm thực tế
            User result = userService.login("testuser", "123456");

            // Kiểm tra kết quả
            assertNotNull(result, "User không được null khi login thành công");
            assertEquals("testuser", result.getUsername());
            assertEquals("BIDDER", result.getRole());

            // Lấy đối tượng DAO giả đã được tạo ra để verify
            UserDAO constructedMock = mockedDao.constructed().get(0);
            verify(constructedMock, times(1)).authenticateUser("testuser", "123456");
        }
    }

    @Test
    public void testLogin_Failure_WrongPassword() {
        try (MockedConstruction<UserDAO> mockedDao = mockConstruction(UserDAO.class,
                (mock, context) -> {
                    when(mock.authenticateUser("testuser", "wrongpass")).thenReturn(null);
                })) {

            UserService userService = new UserService();
            User result = userService.login("testuser", "wrongpass");

            assertNull(result, "User phải là null khi sai mật khẩu");
        }
    }

    // ==========================================
    // TEST LOGIC ĐĂNG KÝ (REGISTER)
    // ==========================================

    @Test
    public void testRegister_Success_AsBidder() {
        try (MockedConstruction<UserDAO> mockedDao = mockConstruction(UserDAO.class,
                (mock, context) -> {
                    when(mock.registerUser("newuser", "pass", "new@gmail.com", "BIDDER")).thenReturn(10L);
                })) {

            UserService userService = new UserService();
            boolean isSuccess = userService.register("newuser", "pass", "new@gmail.com", "BIDDER");

            assertTrue(isSuccess, "Hàm register phải trả về true khi đăng ký thành công");

            UserDAO constructedMock = mockedDao.constructed().get(0);
            verify(constructedMock, times(1)).registerUser("newuser", "pass", "new@gmail.com", "BIDDER");
        }
    }

    @Test
    public void testRegister_Failure_DuplicateUser() {
        try (MockedConstruction<UserDAO> mockedDao = mockConstruction(UserDAO.class,
                (mock, context) -> {
                    when(mock.registerUser("existuser", "pass", "exist@gmail.com", "BIDDER")).thenReturn(null);
                })) {

            UserService userService = new UserService();
            boolean isSuccess = userService.register("existuser", "pass", "exist@gmail.com", "BIDDER");

            assertFalse(isSuccess, "Hàm register phải trả về false khi bị trùng dữ liệu");
        }
    }

    @Test
    public void testRegister_BlockAdminRoleCreation() {
        // Không cần truyền kịch bản vào mockConstruction vì ta expect hàm DAO không được gọi
        try (MockedConstruction<UserDAO> mockedDao = mockConstruction(UserDAO.class)) {

            UserService userService = new UserService();
            boolean isSuccess = userService.register("hacker", "123", "hack@gmail.com", "ADMIN");

            assertFalse(isSuccess, "Hệ thống phải chặn đứng việc tự đăng ký tài khoản ADMIN");

            // Kiểm tra xem UserDAO có thực sự bị "bỏ qua" (không gọi tới hàm registerUser) hay không
            if (!mockedDao.constructed().isEmpty()) {
                UserDAO constructedMock = mockedDao.constructed().get(0);
                verify(constructedMock, never()).registerUser(anyString(), anyString(), anyString(), anyString());
            }
        }
    }
}