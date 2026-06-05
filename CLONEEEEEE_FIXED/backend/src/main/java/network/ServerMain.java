package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.AdminDAO;
import dao.AuctionDAO;
import dao.WalletTransactionDAO;
import dto.ResponsePayload;
import dto.WalletTransactionDTO;
import service.AuctionManager;
import service.AuctionSettlementService;
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
    private static final AuctionSettlementService auctionSettlementService = new AuctionSettlementService();
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

    public static boolean sendToUser(Long userId, ResponsePayload payload) {
        if (userId == null || payload == null) {
            return false;
        }

        String jsonMsg = gson.toJson(payload);
        boolean delivered = false;
        for (ClientHandler client : activeClients) {
            if (userId.equals(client.getAuthenticatedUserId())) {
                client.sendMessage(jsonMsg);
                delivered = true;
            }
        }
        return delivered;
    }

    public static void publishSettlementResults(List<AuctionDAO.SettlementResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        for (AuctionDAO.SettlementResult result : results) {
            if (result == null) {
                continue;
            }

            System.out.println("[Settlement] auctionId=" + result.auctionId
                    + ", item=" + result.itemName
                    + ", winnerId=" + result.winnerId
                    + ", sellerId=" + result.sellerId
                    + ", finalPrice=" + result.finalPrice);

            broadcast(ResponsePayload.success("AUCTION_SETTLED_EVENT", "Auction settled", result));
            broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Auction settled", result.auctionId));

            for (AuctionDAO.SettlementNotification notification : result.notifications) {
                sendSettlementNotification(result, notification);
            }

            if (result.hasWinner) {
                sendBalanceUpdate(result.winnerId);
                pushWalletHistory(result.winnerId);
                sendBalanceUpdate(result.sellerId);
                pushWalletHistory(result.sellerId);
            }
        }
    }

    private static void startAuctionSettlementScheduler(AuctionManager auctionManager) {
        SETTLEMENT_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                List<AuctionDAO.SettlementResult> results = auctionSettlementService.finishExpiredAuctionsAndGetResults();
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

        boolean delivered = sendToUser(userId, ResponsePayload.success("NOTIFICATION_EVENT", title, data));
        System.out.println("[Notification] userId=" + userId
                + ", type=" + type
                + ", auctionId=" + auctionId
                + ", deliveredOnline=" + delivered);
    }

    private static void sendSettlementNotification(
            AuctionDAO.SettlementResult result,
            AuctionDAO.SettlementNotification notification
    ) {
        if (result == null || notification == null) {
            return;
        }

        String type = notification.type;
        Long userId = notification.userId;
        String title = notification.title;
        String message = notification.message;

        if ("AUCTION_WON".equals(type)) {
            if (result.winnerId == null || !result.winnerId.equals(userId)) {
                return;
            }
            title = "Ban da thang phien dau gia";
            message = "Ban da thang " + productLabel(result)
                    + " voi gia " + formatCurrency(result.finalPrice)
                    + ". So tien da duoc tru khoi tai khoan.";
        } else if ("AUCTION_SOLD".equals(type)) {
            if (result.sellerId == null || !result.sellerId.equals(userId)) {
                return;
            }
            title = "San pham cua ban da duoc ban";
            message = productLabel(result)
                    + " da ban thanh cong voi gia " + formatCurrency(result.finalPrice)
                    + ". Tien da duoc cong vao tai khoan.";
        } else if ("AUCTION_NO_WINNER".equals(type)) {
            if (result.sellerId == null || !result.sellerId.equals(userId)) {
                return;
            }
            title = "Phien dau gia ket thuc khong co nguoi thang";
            message = productLabel(result) + " da ket thuc nhung khong co luot bid hop le.";
        } else if ("AUCTION_LOST".equals(type)) {
            if (result.winnerId != null && result.winnerId.equals(userId)) {
                return;
            }
            title = "Phien dau gia da ket thuc";
            message = "Ban khong thang " + productLabel(result)
                    + ". So du kha dung cua ban da duoc cap nhat.";
        }

        sendNotification(
                userId,
                type,
                result.auctionId,
                result.itemName,
                result.finalPrice,
                title,
                message
        );
    }

    private static String productLabel(AuctionDAO.SettlementResult result) {
        if (result.itemName != null && !result.itemName.isBlank()) {
            return result.itemName;
        }
        return "San pham #" + result.itemId;
    }

    private static String formatCurrency(double value) {
        return "$" + String.format(java.util.Locale.US, "%.2f", value);
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

    public static void pushWalletHistory(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            List<WalletTransactionDTO> walletHistory = new WalletTransactionDAO()
                    .getTransactionsByUser(userId)
                    .stream()
                    .map(WalletTransactionDTO::from)
                    .toList();

            sendToUser(
                    userId,
                    ResponsePayload.success(
                            "GET_WALLET_TRANSACTIONS_RESPONSE",
                            "Wallet history updated.",
                            walletHistory
                    )
            );
        } catch (Exception e) {
            System.err.println("Cannot push wallet history to user " + userId + ": " + e.getMessage());
        }
    }
}
