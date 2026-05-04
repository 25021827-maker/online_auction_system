package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root";


    private static DatabaseConnection instance;
    private Connection connection;


    private DatabaseConnection() {
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Đã kết nối thành công tới Database!");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Lỗi kết nối Database: " + e.getMessage());
        }
    }


    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }


    public Connection getConnection() {
        return connection;
    }
}