package dao;

import model.WalletTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class WalletTransactionDAOTest {

    private WalletTransactionDAO walletTxDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseConnection> mockedDbConnection;

    @BeforeEach
    public void setUp() throws Exception {
        walletTxDAO = new WalletTransactionDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        DatabaseConnection mockDbInstance = mock(DatabaseConnection.class);
        when(mockDbInstance.getConnection()).thenReturn(mockConnection);

        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getInstance).thenReturn(mockDbInstance);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @AfterEach
    public void tearDown() {
        mockedDbConnection.close();
    }

    @Test
    public void testGetTransactionsByUser_ReturnsList() throws Exception {
        // KỊCH BẢN: ResultSet có đúng 1 dòng dữ liệu, vòng lặp thứ 2 sẽ ngắt
        when(mockResultSet.next()).thenReturn(true, false);

        // Giả lập dữ liệu của 1 dòng Transaction
        when(mockResultSet.getLong("transaction_id")).thenReturn(1L);
        when(mockResultSet.getString("type")).thenReturn("BID_DEDUCT");
        when(mockResultSet.getDouble("amount")).thenReturn(50.0);
        when(mockResultSet.getString("note")).thenReturn("Tru tien dat gia");
        when(mockResultSet.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));

        List<WalletTransaction> transactions = walletTxDAO.getTransactionsByUser(10L);

        // Kiểm tra
        assertNotNull(transactions);
        assertEquals(1, transactions.size()); // Phải có 1 phần tử
        assertEquals("BID_DEDUCT", transactions.get(0).getType());
        assertEquals(50.0, transactions.get(0).getAmount());

        // Kiểm tra DAO có bind userId = 10L vào câu SQL không
        verify(mockPreparedStatement).setLong(1, 10L);
    }
}