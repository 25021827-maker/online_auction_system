package dao;

import model.Bidder;
import model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserDAOTest {

    private UserDAO userDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseConnection> mockedDbConnection;

    @BeforeEach
    public void setUp() throws Exception {
        userDAO = new UserDAO();

        // 1. Khởi tạo các đói tượng giả (Mocks)
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // Làm giả class Singleton DatabaseConnection
        DatabaseConnection mockDbInstance = mock(DatabaseConnection.class);
        when(mockDbInstance.getConnection()).thenReturn(mockConnection);

        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getInstance).thenReturn(mockDbInstance);

        // Thiết lập PreparedStatement trả về ResultSet giả
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @AfterEach
    public void tearDown() {
        // Đóng mock static để không ảnh hưởng tới test khác
        mockedDbConnection.close();
    }

    @Test
    public void testAuthenticateUser_Success() throws Exception {
        // Giả lập ResultSet có trả về 1 dòng dữ liệu (User tồn tại)
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong("user_id")).thenReturn(1L);
        // Mật khẩu giả là '123' dạng thô để qua hàm check nhanh (do hàm của bạn hỗ trợ plain text)
        when(mockResultSet.getString("password")).thenReturn("123");
        when(mockResultSet.getString("username")).thenReturn("testuser");
        when(mockResultSet.getBoolean("is_admin")).thenReturn(false);
        when(mockResultSet.getBoolean("is_seller")).thenReturn(false);
        when(mockResultSet.getDouble("balance")).thenReturn(500.0);
        when(mockResultSet.getString("role")).thenReturn("BIDDER");

        // Gọi hàm cần test
        User result = userDAO.authenticateUser("testuser", "123");

        // Kiểm tra
        assertNotNull(result);
        assertTrue(result instanceof Bidder);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals(500.0, result.getBalance());
    }

    @Test
    public void testAuthenticateUser_WrongPassword_ReturnsNull() throws Exception {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("password")).thenReturn("correct_password");

        User result = userDAO.authenticateUser("testuser", "wrong_password");

        assertNull(result, "Phai tra ve null neu sai mat khau");
    }

    @Test
    public void testAuthenticateUser_UserNotFound_ReturnsNull() throws Exception {
        // Giả lập không tìm thấy user trong DB (rs.next() = false)
        when(mockResultSet.next()).thenReturn(false);

        User result = userDAO.authenticateUser("unknown", "123");

        assertNull(result, "Phai tra ve null neu khong ton tai user");
    }
}