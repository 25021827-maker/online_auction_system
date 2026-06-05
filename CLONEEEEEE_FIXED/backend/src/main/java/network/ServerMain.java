package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.AdminDAO;
import dao.AuctionDAO;
import dto.ResponsePayload;
import service.AuctionManager;
import util.VietnamTime;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    private static final int PORT = 8080;
    private static final long SETTLEMENT_INITIAL_DELAY_SECONDS = 0;
    private static final long SETTLEMENT_INTERVAL_SECONDS = 5;

    public static final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();
    private static final Gson gson = new Gson();
    private static final AdminDAO adminDAO = new AdminDAO();
    private static final ScheduledExecutorService SETTLEMENT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "auction-settlement-scheduler");
                thread.setDaemon(true);
                return thread;
            });

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(VietnamTime.ZONE));
        System.out.println("Dang khoi dong Server dau gia...");

        AuctionManager auctionManager = new AuctionManager();
        startAuctionSettlementScheduler(auctionManager);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server dang lang nghe tai cong " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Co client moi ket noi: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, auctionManager);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Loi Server: " + e.getMessage());
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

    public static void publishSettlementResults(List<AuctionDAO.SettlementResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        for (AuctionDAO.SettlementResult result : results) {
            if (result == null) {
                continue;
            }

            broadcast(ResponsePayload.success("AUCTION_SETTLED_EVENT", "Auction settled", result));
            broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Auction settled", result.auctionId));

            if (result.hasWinner) {
                sendNotification(
                        result.winnerId,
                        result.winnerNotificationType,
                        result.auctionId,
                        result.itemName,
                        result.finalPrice,
                        result.winnerNotificationTitle,
                        result.winnerNotificationMessage
                );
                sendNotification(
                        result.sellerId,
                        result.sellerNotificationType,
                        result.auctionId,
                        result.itemName,
                        result.finalPrice,
                        result.sellerNotificationTitle,
                        result.sellerNotificationMessage
                );
                sendBalanceUpdate(result.winnerId);
                sendBalanceUpdate(result.sellerId);
            } else {
                sendNotification(
                        result.sellerId,
                        result.sellerNotificationType,
                        result.auctionId,
                        result.itemName,
                        result.finalPrice,
                        result.sellerNotificationTitle,
                        result.sellerNotificationMessage
                );
            }
        }
    }

    private static void startAuctionSettlementScheduler(AuctionManager auctionManager) {
        SETTLEMENT_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                List<AuctionDAO.SettlementResult> results = new AuctionDAO().finishExpiredAuctionsAndGetResults();
                if (!results.isEmpty()) {
                    auctionManager.reloadActiveAuctions();
                    publishSettlementResults(results);
                }
            } catch (Exception e) {
                System.err.println("Loi scheduler settle auction: " + e.getMessage());
            }
        }, SETTLEMENT_INITIAL_DELAY_SECONDS, SETTLEMENT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static void sendNotification(
            Long userId,
            String type,
            Long auctionId,
            String itemName,
            double finalPrice,
            String title,
            String message
    ) {
        if (userId == null || title == null || message == null) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("type", type);
        data.addProperty("auctionId", auctionId);
        data.addProperty("itemName", itemName);
        data.addProperty("finalPrice", finalPrice);
        data.addProperty("title", title);
        data.addProperty("message", message);

        sendToUser(userId, ResponsePayload.success("NOTIFICATION_EVENT", title, data));
    }

    private static void sendBalanceUpdate(Long userId) {
        if (userId == null) {
            return;
        }

        Double balance = adminDAO.getUserBalance(userId);
        if (balance == null) {
            return;
        }

        Double availableBalance = adminDAO.getUserAvailableBalance(userId);
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        data.addProperty("balance", balance);
        data.addProperty("availableBalance", availableBalance != null ? availableBalance : balance);

        sendToUser(userId, ResponsePayload.success("BALANCE_UPDATE", "Balance updated.", data));
    }
}
