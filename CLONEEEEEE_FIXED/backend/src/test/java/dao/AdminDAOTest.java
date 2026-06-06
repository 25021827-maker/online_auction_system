package dao;

import dto.AdminDashboardDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AdminDAOTest {

    private AdminDAO adminDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseConnection> mockedDbConnection;

    @BeforeEach
    public void setUp() throws Exception {
        adminDAO = new AdminDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);
        mockResultSet = mock(ResultSet.class);

        DatabaseConnection mockDbInstance = mock(DatabaseConnection.class);
        when(mockDbInstance.getConnection()).thenReturn(mockConnection);

        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getInstance).thenReturn(mockDbInstance);
    }

    @AfterEach
    public void tearDown() {
        mockedDbConnection.close();
    }

    // ==========================================
    // TEST DUYỆT SẢN PHẨM (UPDATE)
    // ==========================================
    @Test
    public void testApproveProduct_Success() throws Exception {
        // Giả lập prepareStatement luôn trả về statement giả
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        // Giả lập câu lệnh UPDATE thành công tác động tới 1 dòng (return 1)
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = adminDAO.approveProduct(100L);

        assertTrue(result, "Hàm phải trả về true khi update thành công");
        // Xác minh xem DAO có set đúng auction_id vào dấu chấm hỏi (?) thứ nhất không
        verify(mockPreparedStatement).setLong(1, 100L);
    }

    @Test
    public void testApproveProduct_Failure_NotFound() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        // Giả lập không có dòng nào được update (auction không tồn tại)
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        boolean result = adminDAO.approveProduct(999L);

        assertFalse(result, "Hàm phải trả về false khi không có dòng nào được update");
    }

    // ==========================================
    // TEST DASHBOARD (SELECT)
    // ==========================================
    @Test
    public void testGetDashboard_Success() throws Exception {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

        // Giả lập rs.next() trả về true để đọc dữ liệu
        when(mockResultSet.next()).thenReturn(true);
        // Giả lập rs.getLong(1) trả về số đếm là 5 (ví dụ: 5 users, 5 auctions...)
        when(mockResultSet.getLong(1)).thenReturn(5L);
        when(mockResultSet.getDouble(1)).thenReturn(1500.0); // Giả lập tổng tiền

        AdminDashboardDTO dto = adminDAO.getDashboard();

        assertNotNull(dto);
        assertEquals(5L, dto.totalUsers);
        assertEquals(5L, dto.totalAuctions);
        assertEquals(1500.0, dto.totalUserBalance);
    }
}