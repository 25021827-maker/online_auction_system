package network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import dto.ResponsePayload;
import com.google.gson.Gson;

import service.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 8080;

    public static final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("Đang khởi động Server đấu giá...");

        // Khởi tạo Manager ở đây để tải DB 1 lần duy nhất khi bật server
        AuctionManager auctionManager = new AuctionManager();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có client mới kết nối: " + clientSocket.getInetAddress());

                // Truyền auctionManager vào ClientHandler
                ClientHandler handler = new ClientHandler(clientSocket, auctionManager);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }

    public static void broadcast(ResponsePayload payload) {
        String jsonMsg = gson.toJson(payload);
        for (ClientHandler client : activeClients) {
            client.sendMessage(jsonMsg);
        }
    }

    public static void sendToUser(Long userId, ResponsePayload payload) {
        if (userId == null || payload == null) {
            return;
        }

        String jsonMsg = gson.toJson(payload);
        for (ClientHandler client : activeClients) {
            if (userId.equals(client.getAuthenticatedUserId())) {
                client.sendMessage(jsonMsg);
            }
        }
    }

}
