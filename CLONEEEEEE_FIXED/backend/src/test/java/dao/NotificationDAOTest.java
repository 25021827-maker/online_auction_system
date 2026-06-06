package dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class NotificationDAOTest {

    private NotificationDAO notificationDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseConnection> mockedDbConnection;

    @BeforeEach
    public void setUp() throws Exception {
        notificationDAO = new NotificationDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
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

    @Test
    public void testGetUnreadCount_ReturnsCorrectNumber() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(3); // Giả lập đếm ra 3 thông báo

        int unreadCount = notificationDAO.getUnreadCount(10L);

        assertEquals(3, unreadCount);
        verify(mockPreparedStatement).setLong(1, 10L);
    }

    @Test
    public void testMarkAsRead_Success() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // 1 thông báo được update

        boolean result = notificationDAO.markAsRead(10L, 100L); // userId=10, notifId=100

        assertTrue(result);
        verify(mockPreparedStatement).setLong(1, 100L); // Dấu ? thứ nhất là notifId
        verify(mockPreparedStatement).setLong(2, 10L);  // Dấu ? thứ hai là userId
    }
}