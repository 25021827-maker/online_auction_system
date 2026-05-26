package network;

import service.AuctionService;
import service.AuctionScheduler;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        AuctionSubject subject = new AuctionSubject();
        AuctionService auctionService = new AuctionService();
        // Sửa: truyền cả subject và auctionService vào scheduler
        AuctionScheduler scheduler = new AuctionScheduler(subject, auctionService);
        scheduler.start();

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket client = server.accept();
                ClientHandler handler = new ClientHandler(client, subject, auctionService);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}