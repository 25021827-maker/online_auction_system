package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("Đang khởi động Server đấu giá...");


        AuctionSubject globalSubject = new AuctionSubject();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe tại cổng " + PORT);

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Có client mới kết nối: " + clientSocket.getInetAddress());


                ClientHandler handler = new ClientHandler(clientSocket, globalSubject);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }
}