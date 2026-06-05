package network;

import com.google.gson.*;
import dao.AdminDAO;
import dao.AuctionDAO;
import dao.NotificationDAO;
import dao.WalletTransactionDAO;
import dto.*;
import model.Auction;
import model.BidTransaction;
import model.User;
import service.AuctionManager;
import service.AuctionSettlementService;
import service.UserService;
import util.VietnamTime;
import service.ImageStorageService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClientHandler implements Runnable {
    private static final long MIN_AUCTION_DURATION_MINUTES = 5;
    private static final int LOGIN_NOTIFICATION_REPLAY_LIMIT = 5;
    private final Socket clientSocket;
    private final AuctionManager auctionManager;
    private final UserService userService;
    private final AdminDAO adminDAO;
    private final NotificationDAO notificationDAO;
    private final AuctionSettlementService auctionSettlementService;
    private final Gson gson;
    private PrintWriter out;
    private BufferedReader in;
    private volatile User authenticatedUser;

    private static final long AUTO_BID_DELAY_SECONDS = 2;

    private static final ScheduledExecutorService AUTO_BID_SCHEDULER =
            Executors.newScheduledThreadPool(2);

    private static final ConcurrentHashMap<Long, ScheduledFuture<?>> PENDING_AUTO_BID_TASKS =
            new ConcurrentHashMap<>();

    public ClientHandler(Socket socket, AuctionManager manager) {
        this.clientSocket = socket;
        this.auctionManager = manager;
        this.userService = new UserService();
        this.adminDAO = new AdminDAO();
        this.notificationDAO = new NotificationDAO();
        this.auctionSettlementService = new AuctionSettlementService();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                        (json, type, context) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                        (src, type, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();
        ServerMain.activeClients.add(this);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                RequestPayload request = gson.fromJson(requestLine, RequestPayload.class);
                if (request != null && request.getAction() != null) {
                    handleRequest(request);
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            ServerMain.activeClients.remove(this);
            AuctionRoomManager.removeClientFromAllRooms(this);
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }

    public synchronized void sendMessage(String jsonMsg) {
        if (out != null) out.println(jsonMsg);
    }

    public Long getAuthenticatedUserId() {
        return authenticatedUser == null ? null : authenticatedUser.getId();
    }

    private void handleRequest(RequestPayload request) {
        try {
            switch (request.getAction()) {
                case "LOGIN" -> processLogin(request.getData());
                case "REGISTER" -> processRegister(request.getData());
                case "PLACE_BID" -> processBid(request.getData());
                case "SET_AUTO_BID" -> processSetAutoBid(request.getData());
                case "GET_ACTIVE_AUCTIONS" -> processGetActiveAuctions();
                case "CREATE_AUCTION" -> processCreateAuction(request.getData());
                case "UPDATE_AUCTION" -> processUpdateAuction(request.getData());
                case "GET_MY_PRODUCTS" -> processGetMyProducts(request.getData());
                case "DELETE_PRODUCT" -> processDeleteProduct(request.getData());
                case "GET_WATCHLIST" -> processGetWatchlist(request.getData());
                case "ADD_WATCHLIST" -> processAddWatchlist(request.getData());
                case "REMOVE_WATCHLIST" -> processRemoveWatchlist(request.getData());
                case "GET_WATCHLIST_IDS" -> processGetWatchlistIds(request.getData());
                case "SUBMIT_DEPOSIT" -> processSubmitDeposit(request.getData());
                case "ADD_BALANCE" -> processAddBalance(request.getData());
                case "GET_BALANCE" -> processGetBalance();
                case "GET_NOTIFICATIONS" -> processGetNotifications();
                case "GET_UNREAD_NOTIFICATION_COUNT" -> processGetUnreadNotificationCount();
                case "MARK_NOTIFICATION_READ" -> processMarkNotificationRead(request.getData());
                case "GET_WALLET_TRANSACTIONS" -> processGetWalletTransactions(request.getData());
                case "GET_MY_WIN_LIST" -> processGetMyWinList(request.getData());
                case "GET_MY_SOLD_LIST" -> processGetMySoldList(request.getData());
                case "GET_BID_HISTORY" -> processGetBidHistory(request.getData());
                case "JOIN_AUCTION_ROOM" -> processJoinAuctionRoom(request.getData());
                case "LEAVE_AUCTION_ROOM" -> processLeaveAuctionRoom(request.getData());
                case "GET_ADMIN_DASHBOARD" -> {
                    if (requireAdmin("GET_ADMIN_DASHBOARD_RESPONSE")) {
                        sendResponse(ResponsePayload.success("GET_ADMIN_DASHBOARD_RESPONSE", "OK", adminDAO.getDashboard()));
                    }
                }
                case "GET_ADMIN_USERS" -> {
                    if (requireAdmin("GET_ADMIN_USERS_RESPONSE")) {
                        sendResponse(ResponsePayload.success("GET_ADMIN_USERS_RESPONSE", "OK", adminDAO.getUsers()));
                    }
                }
                case "ADMIN_SET_USER_ACTIVE" -> processAdminSetUserActive(request.getData());
                case "GET_ADMIN_DEPOSITS" -> {
                    if (requireAdmin("GET_ADMIN_DEPOSITS_RESPONSE")) {
                        sendResponse(ResponsePayload.success("GET_ADMIN_DEPOSITS_RESPONSE", "OK", adminDAO.getDepositRequests()));
                    }
                }
                case "ADMIN_APPROVE_DEPOSIT" -> processAdminReviewDeposit(request.getData(), true);
                case "ADMIN_REJECT_DEPOSIT" -> processAdminReviewDeposit(request.getData(), false);
                case "GET_ADMIN_PRODUCTS" -> {
                    if (requireAdmin("GET_ADMIN_PRODUCTS_RESPONSE")) {
                        sendResponse(ResponsePayload.success("GET_ADMIN_PRODUCTS_RESPONSE", "OK", adminDAO.getProductsForReview()));
                    }
                }
                case "ADMIN_APPROVE_PRODUCT" -> processAdminReviewProduct(request.getData(), true);
                case "ADMIN_REJECT_PRODUCT" -> processAdminReviewProduct(request.getData(), false);
                case "GET_ADMIN_AUCTIONS" -> {
                    if (requireAdmin("GET_ADMIN_AUCTIONS_RESPONSE")) {
                        sendResponse(ResponsePayload.success("GET_ADMIN_AUCTIONS_RESPONSE", "OK", adminDAO.getAuctions()));
                    }
                }
                case "GET_ADMIN_WINNERS" -> {
                    if (requireAdmin("GET_ADMIN_WINNERS_RESPONSE")) {
                        sendResponse(ResponsePayload.success("GET_ADMIN_WINNERS_RESPONSE", "OK", adminDAO.getWinners()));
                    }
                }
                case "ADMIN_UPDATE_AUCTION_STATUS" -> processAdminUpdateAuctionStatus(request.getData());
                default -> sendResponse(ResponsePayload.fail(request.getAction() + "_RESPONSE", "Hanh dong khong hop le: " + request.getAction()));
            }
        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(request.getAction() + "_RESPONSE", "Loi Server: " + e.getMessage()));
        }
    }

    private JsonObject parseObject(String jsonData) {
        return gson.fromJson(jsonData == null || jsonData.isBlank() ? "{}" : jsonData, JsonObject.class);
    }

    private void sendResponse(ResponsePayload response) {
        sendMessage(gson.toJson(response));
    }

    private Long parseAuctionId(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return null;
        }

        String trimmed = dataJson.trim();
        try {
            if (trimmed.startsWith("{")) {
                JsonObject json = parseObject(trimmed);
                return json.has("auctionId") ? json.get("auctionId").getAsLong() : null;
            }
            return Long.parseLong(trimmed.replace("\"", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private void processJoinAuctionRoom(String dataJson) {
        Long auctionId = parseAuctionId(dataJson);
        if (auctionId == null) {
            sendResponse(ResponsePayload.fail("JOIN_AUCTION_ROOM_RESPONSE", "auctionId khong hop le."));
            return;
        }

        AuctionRoomManager.joinRoom(auctionId, this);
        sendResponse(ResponsePayload.success("JOIN_AUCTION_ROOM_RESPONSE", "Joined auction room.", auctionId));
    }

    private void processLeaveAuctionRoom(String dataJson) {
        Long auctionId = parseAuctionId(dataJson);
        if (auctionId == null) {
            sendResponse(ResponsePayload.fail("LEAVE_AUCTION_ROOM_RESPONSE", "auctionId khong hop le."));
            return;
        }

        AuctionRoomManager.leaveRoom(auctionId, this);
        sendResponse(ResponsePayload.success("LEAVE_AUCTION_ROOM_RESPONSE", "Left auction room.", auctionId));
    }

    private boolean requireLoggedIn(String responseAction) {
        if (authenticatedUser != null) {
            return true;
        }
        sendResponse(ResponsePayload.fail(responseAction, "Vui long dang nhap truoc khi thuc hien thao tac nay."));
        return false;
    }

    private boolean requireAdmin(String responseAction) {
        if (authenticatedUser != null && "ADMIN".equalsIgnoreCase(authenticatedUser.getRole())) {
            return true;
        }
        sendResponse(ResponsePayload.fail(responseAction, "Ban khong co quyen admin."));
        return false;
    }

    private boolean requireSameUser(Long userId, String responseAction) {
        if (!requireLoggedIn(responseAction)) {
            return false;
        }
        if (userId != null && userId.equals(authenticatedUser.getId())) {
            return true;
        }
        sendResponse(ResponsePayload.fail(responseAction, "Khong duoc thao tac thay nguoi dung khac."));
        return false;
    }

    private boolean requireSeller(Long sellerId, String responseAction) {
        if (!requireSameUser(sellerId, responseAction)) {
            return false;
        }
        if ("SELLER".equalsIgnoreCase(authenticatedUser.getRole())) {
            return true;
        }
        sendResponse(ResponsePayload.fail(responseAction, "Chi tai khoan SELLER moi duoc thuc hien thao tac nay."));
        return false;
    }

    private void processLogin(String jsonData) {
        LoginRequest loginReq = gson.fromJson(jsonData, LoginRequest.class);
        User loggedInUser = userService.login(loginReq.username, loginReq.password);
        authenticatedUser = loggedInUser;

        if (loggedInUser == null) {
            sendResponse(ResponsePayload.fail("LOGIN_RESPONSE", "Sai ten dang nhap hoac mat khau"));
            return;
        }

        sendResponse(ResponsePayload.success("LOGIN_RESPONSE", "Dang nhap thanh cong", loggedInUser));
        replayUnreadNotificationsAfterLogin(loggedInUser.getId());
    }

    private void replayUnreadNotificationsAfterLogin(Long userId) {
        List<NotificationDAO.NotificationRecord> unreadNotifications =
                notificationDAO.getUnreadNotifications(userId, LOGIN_NOTIFICATION_REPLAY_LIMIT);

        for (NotificationDAO.NotificationRecord notification : unreadNotifications) {
            JsonObject data = new JsonObject();
            String resolvedType = notificationDAO.resolveAuctionNotificationType(
                    userId,
                    notification.auctionId,
                    notification.type
            );
            String title = replayTitle(resolvedType, notification);
            String message = replayMessage(resolvedType, notification);

            data.addProperty("notificationId", notification.notificationId);
            data.addProperty("type", resolvedType);
            data.addProperty("auctionId", notification.auctionId);
            data.addProperty("title", title);
            data.addProperty("message", message);
            data.addProperty("createdAt", notification.createdAt);

            sendResponse(ResponsePayload.success(
                    "NOTIFICATION_EVENT",
                    title,
                    data
            ));
        }
    }

    private String replayTitle(String type, NotificationDAO.NotificationRecord notification) {
        if (type == null) {
            return "Notification";
        }

        return switch (type) {
            case "AUCTION_WON" -> "Ban da thang phien dau gia";
            case "AUCTION_SOLD" -> "San pham cua ban da duoc ban";
            case "AUCTION_NO_WINNER" -> "Phien dau gia ket thuc khong co nguoi thang";
            case "AUCTION_LOST" -> "Phien dau gia da ket thuc";
            default -> notification.title == null ? "Notification" : notification.title;
        };
    }

    private String replayMessage(String type, NotificationDAO.NotificationRecord notification) {
        if (notification == null || type == null) {
            return "";
        }

        String auctionLabel = notification.auctionId == null
                ? "phien dau gia"
                : "auction #" + notification.auctionId;

        return switch (type) {
            case "AUCTION_WON" -> "Ban da thang " + auctionLabel + ". So tien da duoc tru khoi tai khoan.";
            case "AUCTION_SOLD" -> "San pham cua ban trong " + auctionLabel + " da ban thanh cong.";
            case "AUCTION_NO_WINNER" -> auctionLabel + " da ket thuc nhung khong co luot bid hop le.";
            case "AUCTION_LOST" -> "Ban khong thang " + auctionLabel + ". So du kha dung cua ban da duoc cap nhat.";
            default -> notification.message == null ? "" : notification.message;
        };
    }

    private void processRegister(String jsonData) {
        RegisterRequest req = gson.fromJson(jsonData, RegisterRequest.class);
        boolean isSuccess = userService.register(req.username, req.password, req.email, req.role);
        sendResponse(isSuccess
                ? ResponsePayload.success("REGISTER_RESPONSE", "Dang ky thanh cong", null)
                : ResponsePayload.fail("REGISTER_RESPONSE", "Dang ky that bai. Ten dang nhap hoac email co the da ton tai."));
    }

    private void processBid(String jsonData) {
        BidRequest bidReq = gson.fromJson(jsonData, BidRequest.class);

        try {
            if (bidReq == null || !requireSameUser(bidReq.bidderId, "PLACE_BID_RESPONSE")) {
                return;
            }

            AuctionDAO.AuctionBidState beforeBidState = new AuctionDAO().getAuctionBidState(bidReq.auctionId);
            Long previousLeaderId = beforeBidState == null ? null : beforeBidState.currentLeaderId;

            BidTransaction newBid = new BidTransaction(null, bidReq.bidderId, bidReq.amount);

            boolean ok = auctionManager.processBid(bidReq.auctionId, newBid);
            bidReq.autoBid = false;

            if (!ok) {
                sendResponse(ResponsePayload.fail("PLACE_BID_RESPONSE", "Gia dat khong hop le hoac phien da dong."));
                return;
            }

            broadcastBidEvent(bidReq, "New bid placed");
            broadcastAuctionTimeIfChanged(bidReq.auctionId);

            sendResponse(ResponsePayload.success("PLACE_BID_RESPONSE", "Dat gia thanh cong!", null));

            sendBalanceUpdate(bidReq.bidderId);
            ServerMain.pushWalletHistory(bidReq.bidderId);

            if (previousLeaderId != null && !previousLeaderId.equals(bidReq.bidderId)) {
                sendBalanceUpdate(previousLeaderId);
                ServerMain.pushWalletHistory(previousLeaderId);
            }

            scheduleDelayedAutoBid(bidReq.auctionId);

        } catch (exception.InvalidBidException | exception.AuctionClosedException e) {
            sendResponse(ResponsePayload.fail("PLACE_BID_RESPONSE", e.getMessage()));
        }
    }

    private void processSetAutoBid(String jsonData) {
        AutoBidRequest req = gson.fromJson(jsonData, AutoBidRequest.class);

        try {
            if (req == null || !requireSameUser(req.bidderId, "SET_AUTO_BID_RESPONSE")) {
                return;
            }

            auctionManager.configureAutoBid(req);

            sendResponse(ResponsePayload.success("SET_AUTO_BID_RESPONSE", "Auto bid da duoc bat.", null));

            scheduleDelayedAutoBid(req.auctionId);

        } catch (exception.InvalidBidException | exception.AuctionClosedException e) {
            sendResponse(ResponsePayload.fail("SET_AUTO_BID_RESPONSE", e.getMessage()));
        }
    }

    private void scheduleDelayedAutoBid(Long auctionId) {
        if (auctionId == null) {
            return;
        }

        ScheduledFuture<?> oldTask = PENDING_AUTO_BID_TASKS.remove(auctionId);

        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        ScheduledFuture<?> newTask = AUTO_BID_SCHEDULER.schedule(() -> {
            PENDING_AUTO_BID_TASKS.remove(auctionId);

            try {
                AuctionDAO.AuctionBidState beforeAutoBidState = new AuctionDAO().getAuctionBidState(auctionId);
                Long previousLeaderId = beforeAutoBidState == null ? null : beforeAutoBidState.currentLeaderId;

                List<BidRequest> autoBids = auctionManager.processAutoBids(auctionId);

                for (BidRequest autoBid : autoBids) {
                    broadcastBidEvent(autoBid, "Auto bid da dat gia moi");
                    broadcastAuctionTimeIfChanged(autoBid.auctionId);
                    sendBalanceUpdate(autoBid.bidderId);
                    ServerMain.pushWalletHistory(autoBid.bidderId);

                    if (previousLeaderId != null && !previousLeaderId.equals(autoBid.bidderId)) {
                        sendBalanceUpdate(previousLeaderId);
                        ServerMain.pushWalletHistory(previousLeaderId);
                    }
                    previousLeaderId = autoBid.bidderId;
                }

            } catch (Exception e) {
                System.err.println("Loi khi chay delayed auto bid cho auction " + auctionId);
                e.printStackTrace();
            }

        }, AUTO_BID_DELAY_SECONDS, TimeUnit.SECONDS);

        PENDING_AUTO_BID_TASKS.put(auctionId, newTask);
    }

    private void broadcastBidEvent(BidRequest bid, String message) {
        ResponsePayload roomEvent = ResponsePayload.success("BID_UPDATE", message, bid);

        AuctionRoomManager.broadcastToRoom(bid.auctionId, gson.toJson(roomEvent));

        ServerMain.broadcast(ResponsePayload.success("NEW_BID_EVENT", message, bid));

        ServerMain.broadcast(ResponsePayload.success("AUCTION_PRICE_CHANGED", message, bid));
    }

    private void processGetActiveAuctions() {
        List<Auction> activeAuctions = auctionManager.getActiveAuctionsList();
        sendResponse(ResponsePayload.success("GET_ACTIVE_AUCTIONS_RESPONSE", "Thanh cong", toAuctionDTOs(activeAuctions)));
    }

    private void processCreateAuction(String dataJson) {
        try {
            CreateAuctionRequest req = gson.fromJson(dataJson, CreateAuctionRequest.class);
            if (req == null || !requireSeller(req.sellerId, "CREATE_AUCTION_RESPONSE")) {
                return;
            }
            LocalDateTime start = LocalDateTime.parse(req.startTime);
            LocalDateTime end = LocalDateTime.parse(req.endTime);
            validateAuctionSchedule(start, end);
            ImageStorageService imageStorageService = new ImageStorageService();

            String serverImagePath = resolveServerImagePath(req, imageStorageService);

            boolean isSuccess = auctionManager.createAuction(
                    req.sellerId,
                    req.itemName,
                    req.description,
                    req.startingPrice,
                    req.category,
                    req.condition,
                    serverImagePath,
                    start,
                    end
            );

            if (isSuccess) {
                sendResponse(ResponsePayload.success("CREATE_AUCTION_RESPONSE", "Tao phien dau gia thanh cong", null));
                ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Co san pham moi", null));
            } else {
                sendResponse(ResponsePayload.fail("CREATE_AUCTION_RESPONSE", "Khong the tao phien dau gia"));
            }
        } catch (Exception e) {
            sendResponse(ResponsePayload.fail("CREATE_AUCTION_RESPONSE", "Du lieu tao phien khong hop le: " + e.getMessage()));
        }
    }

    private void processUpdateAuction(String dataJson) {
        try {
            CreateAuctionRequest req = gson.fromJson(dataJson, CreateAuctionRequest.class);

            if (req == null || !requireSeller(req.sellerId, "UPDATE_AUCTION_RESPONSE")) {
                return;
            }

            LocalDateTime start = LocalDateTime.parse(req.startTime);
            LocalDateTime end = LocalDateTime.parse(req.endTime);
            validateAuctionSchedule(start, end);

            ImageStorageService imageStorageService = new ImageStorageService();

            String serverImagePath = resolveServerImagePath(req, imageStorageService);

            boolean isSuccess = new AuctionDAO().updateAuction(
                    req.auctionId,
                    req.sellerId,
                    req.itemName,
                    req.description,
                    req.startingPrice,
                    req.category,
                    req.condition,
                    serverImagePath,
                    start,
                    end
            );

            if (isSuccess) {
                auctionManager.reloadActiveAuctions();
                sendResponse(ResponsePayload.success(
                        "UPDATE_AUCTION_RESPONSE",
                        "Cap nhat phien dau gia thanh cong",
                        null
                ));
                ServerMain.broadcast(ResponsePayload.success(
                        "NEW_AUCTION_EVENT",
                        "San pham duoc cap nhat",
                        req.auctionId
                ));
            } else {
                sendResponse(ResponsePayload.fail(
                        "UPDATE_AUCTION_RESPONSE",
                        "Khong the cap nhat phien dau gia."
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ResponsePayload.fail(
                    "UPDATE_AUCTION_RESPONSE",
                    "Du lieu cap nhat khong hop le: " + e.getMessage()
            ));
        }
    }

    private String resolveServerImagePath(CreateAuctionRequest req, ImageStorageService imageStorageService) throws java.io.IOException {
        if (req.imageBase64 != null && !req.imageBase64.isBlank()) {
            return imageStorageService.saveProductImage(req.imageBase64, req.imageFileName);
        }

        if (req.imagePath != null && req.imagePath.startsWith("file:")) {
            String localImageBase64 = imageStorageService.loadImageAsBase64(req.imagePath);
            if (localImageBase64 != null && !localImageBase64.isBlank()) {
                return imageStorageService.saveProductImage(localImageBase64, imageFileNameFromPath(req.imagePath));
            }
        }

        return req.imagePath;
    }

    private String imageFileNameFromPath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalized = imagePath.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private void validateAuctionSchedule(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thoi gian dau gia khong hop le.");
        }
        LocalDateTime now = VietnamTime.now();
        if (start.isBefore(now.minusMinutes(1))) {
            throw new IllegalArgumentException("Thoi gian bat dau khong duoc nam trong qua khu.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau.");
        }
        if (Duration.between(start, end).toMinutes() < MIN_AUCTION_DURATION_MINUTES) {
            throw new IllegalArgumentException("Phien dau gia phai keo dai toi thieu " + MIN_AUCTION_DURATION_MINUTES + " phut.");
        }
    }

    private void processGetMyProducts(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long sellerId = json.get("sellerId").getAsLong();
        if (!requireSeller(sellerId, "GET_MY_PRODUCTS_RESPONSE")) {
            return;
        }
        sendResponse(ResponsePayload.success("GET_MY_PRODUCTS_RESPONSE", "Thanh cong", toAuctionDTOs(new AuctionDAO().getAuctionsBySeller(sellerId))));
    }

    private void processDeleteProduct(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long auctionId = json.get("auctionId").getAsLong();
        Long sellerId = authenticatedUser != null ? authenticatedUser.getId() : null;
        if (!requireSeller(sellerId, "DELETE_PRODUCT_RESPONSE")) {
            return;
        }
        if (new AuctionDAO().deleteAuction(auctionId, sellerId)) {
            sendResponse(ResponsePayload.success("DELETE_PRODUCT_RESPONSE", "Da xoa", null));
            ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "San pham bi xoa", null));
        } else {
            sendResponse(ResponsePayload.fail("DELETE_PRODUCT_RESPONSE", "Khong the xoa san pham."));
        }
    }

    private void processGetWatchlist(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long userId = json.get("userId").getAsLong();

        if (!requireSameUser(userId, "GET_WATCHLIST_RESPONSE")) {
            return;
        }

        sendResponse(ResponsePayload.success(
                "GET_WATCHLIST_RESPONSE",
                "Thanh cong",
                toAuctionDTOs(new AuctionDAO().getWatchlist(userId))
        ));
    }



    private List<AuctionDTO> toAuctionDTOs(List<Auction> auctions) {
        return auctions.stream().map(AuctionDTO::from).toList();
    }

    private void processSubmitDeposit(String dataJson) {
        DepositRequest req = gson.fromJson(dataJson, DepositRequest.class);
        if (req == null || !requireSameUser(req.userId, "SUBMIT_DEPOSIT_RESPONSE")) {
            return;
        }
        boolean ok = req.amount > 0 && adminDAO.submitDepositRequest(req);
        sendResponse(ok
                ? ResponsePayload.success("SUBMIT_DEPOSIT_RESPONSE", "Yeu cau nap tien dang cho admin duyet.", null)
                : ResponsePayload.fail("SUBMIT_DEPOSIT_RESPONSE", "Khong the gui yeu cau nap tien."));
    }

    private void processAdminSetUserActive(String dataJson) {
        if (!requireAdmin("ADMIN_SET_USER_ACTIVE_RESPONSE")) {
            return;
        }
        JsonObject json = parseObject(dataJson);
        Long userId = json.get("userId").getAsLong();
        boolean active = json.get("active").getAsBoolean();
        boolean ok = adminDAO.setUserActive(userId, active);
        sendResponse(ok
                ? ResponsePayload.success("ADMIN_SET_USER_ACTIVE_RESPONSE", "Da cap nhat nguoi dung.", adminDAO.getUsers())
                : ResponsePayload.fail("ADMIN_SET_USER_ACTIVE_RESPONSE", "Khong the cap nhat nguoi dung."));
    }

    private void processAdminReviewDeposit(String dataJson, boolean approve) {
        String action = approve ? "ADMIN_APPROVE_DEPOSIT_RESPONSE" : "ADMIN_REJECT_DEPOSIT_RESPONSE";
        if (!requireAdmin(action)) {
            return;
        }
        JsonObject json = parseObject(dataJson);
        Long requestId = json.get("requestId").getAsLong();
        Long adminId = authenticatedUser.getId();
        AdminDepositDTO deposit = approve ? adminDAO.getDepositRequest(requestId) : null;
        boolean ok = approve ? adminDAO.approveDeposit(requestId, adminId) : adminDAO.rejectDeposit(requestId, adminId);
        if (ok && approve && deposit != null) {
            sendBalanceUpdate(deposit.userId);
            ServerMain.pushWalletHistory(deposit.userId);
        }
        sendResponse(ok
                ? ResponsePayload.success(action, "Da xu ly yeu cau nap tien.", adminDAO.getDepositRequests())
                : ResponsePayload.fail(action, "Khong the xu ly yeu cau nap tien."));
    }

    private void processGetBalance() {
        if (!requireLoggedIn("GET_BALANCE_RESPONSE")) {
            return;
        }
        sendBalanceResponse(authenticatedUser.getId(), "GET_BALANCE_RESPONSE");
    }

    private void processGetNotifications() {
        if (!requireLoggedIn("GET_NOTIFICATIONS_RESPONSE")) {
            return;
        }

        sendResponse(ResponsePayload.success(
                "GET_NOTIFICATIONS_RESPONSE",
                "Notifications loaded.",
                notificationDAO.getNotifications(authenticatedUser.getId())
        ));
    }

    private void processGetUnreadNotificationCount() {
        if (!requireLoggedIn("GET_UNREAD_NOTIFICATION_COUNT_RESPONSE")) {
            return;
        }

        sendResponse(ResponsePayload.success(
                "GET_UNREAD_NOTIFICATION_COUNT_RESPONSE",
                "Unread count loaded.",
                notificationDAO.getUnreadCount(authenticatedUser.getId())
        ));
    }

    private void processMarkNotificationRead(String dataJson) {
        if (!requireLoggedIn("MARK_NOTIFICATION_READ_RESPONSE")) {
            return;
        }

        JsonObject json = parseObject(dataJson);
        Long notificationId = json.has("notificationId")
                ? json.get("notificationId").getAsLong()
                : null;

        boolean ok = notificationId != null
                && notificationDAO.markAsRead(authenticatedUser.getId(), notificationId);

        sendResponse(ok
                ? ResponsePayload.success(
                        "MARK_NOTIFICATION_READ_RESPONSE",
                        "Notification marked as read.",
                        notificationDAO.getUnreadCount(authenticatedUser.getId())
                )
                : ResponsePayload.fail(
                        "MARK_NOTIFICATION_READ_RESPONSE",
                        "Khong the cap nhat notification."
                ));
    }

    private void processGetWalletTransactions(String dataJson) {
        try {
            JsonObject json = parseObject(dataJson);
            Long userId = json.get("userId").getAsLong();

            if (!requireSameUser(userId, "GET_WALLET_TRANSACTIONS_RESPONSE")) {
                return;
            }

            List<WalletTransactionDTO> dtos = new WalletTransactionDAO()
                    .getTransactionsByUser(userId)
                    .stream()
                    .map(WalletTransactionDTO::from)
                    .toList();
            System.out.println("[ClientHandler] Loaded " + dtos.size()
                    + " wallet transaction(s) for userId=" + userId);

            sendResponse(ResponsePayload.success(
                    "GET_WALLET_TRANSACTIONS_RESPONSE",
                    "Thanh cong",
                    dtos
            ));

        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(
                    "GET_WALLET_TRANSACTIONS_RESPONSE",
                    "Khong the lay lich su giao dich: " + e.getMessage()
            ));
        }
    }

    private void processGetMyWinList(String dataJson) {
        try {
            JsonObject json = parseObject(dataJson);
            Long userId = json.get("userId").getAsLong();

            if (!requireSameUser(userId, "GET_MY_WIN_LIST_RESPONSE")) {
                return;
            }

            List<UserAuctionResultDTO> results = new AuctionDAO()
                    .getWinListByBidder(userId);

            sendResponse(ResponsePayload.success(
                    "GET_MY_WIN_LIST_RESPONSE",
                    "Win list loaded.",
                    results
            ));

        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(
                    "GET_MY_WIN_LIST_RESPONSE",
                    "Khong the lay danh sach da thang: " + e.getMessage()
            ));
        }
    }

    private void processGetMySoldList(String dataJson) {
        try {
            JsonObject json = parseObject(dataJson);
            Long userId = json.get("userId").getAsLong();

            if (!requireSameUser(userId, "GET_MY_SOLD_LIST_RESPONSE")) {
                return;
            }

            List<UserAuctionResultDTO> results = new AuctionDAO()
                    .getSoldListBySeller(userId);

            sendResponse(ResponsePayload.success(
                    "GET_MY_SOLD_LIST_RESPONSE",
                    "Sold list loaded.",
                    results
            ));

        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(
                    "GET_MY_SOLD_LIST_RESPONSE",
                    "Khong the lay danh sach da ban: " + e.getMessage()
            ));
        }
    }

    private void sendBalanceUpdate(Long userId) {
        sendBalanceToUser(userId, "BALANCE_UPDATE");
    }

    private void sendBalanceResponse(Long userId, String action) {
        Double balance = adminDAO.getUserBalance(userId);
        if (balance == null) {
            sendResponse(ResponsePayload.fail(action, "Khong the lay so du moi nhat."));
            return;
        }

        JsonObject data = buildBalanceData(userId, balance);
        sendResponse(ResponsePayload.success(action, "Balance loaded.", data));
    }

    private void sendBalanceToUser(Long userId, String action) {
        Double balance = adminDAO.getUserBalance(userId);
        if (balance == null) {
            return;
        }

        ServerMain.sendToUser(userId, ResponsePayload.success(action, "Balance updated.", buildBalanceData(userId, balance)));
    }

    private JsonObject buildBalanceData(Long userId, double balance) {
        Double availableBalance = adminDAO.getUserAvailableBalance(userId);
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        data.addProperty("balance", balance);
        data.addProperty("availableBalance", availableBalance != null ? availableBalance : balance);
        return data;
    }

    private void processAdminReviewProduct(String dataJson, boolean approve) {
        String action = approve ? "ADMIN_APPROVE_PRODUCT_RESPONSE" : "ADMIN_REJECT_PRODUCT_RESPONSE";
        if (!requireAdmin(action)) {
            return;
        }
        JsonObject json = parseObject(dataJson);
        Long auctionId = json.get("auctionId").getAsLong();
        boolean ok = approve ? adminDAO.approveProduct(auctionId) : adminDAO.rejectProduct(auctionId);
        if (ok) {
            auctionManager.reloadActiveAuctions();
            ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Admin da cap nhat san pham", null));
        }
        sendResponse(ok
                ? ResponsePayload.success(action, "Da cap nhat san pham.", adminDAO.getProductsForReview())
                : ResponsePayload.fail(action, "Khong the cap nhat san pham."));
    }

    private void processAdminUpdateAuctionStatus(String dataJson) {
        if (!requireAdmin("ADMIN_UPDATE_AUCTION_STATUS_RESPONSE")) {
            return;
        }
        JsonObject json = parseObject(dataJson);
        Long auctionId = json.get("auctionId").getAsLong();
        String status = json.get("status").getAsString();
        AuctionDAO.SettlementResult settlementResult = null;
        boolean ok;

        if ("FINISHED".equalsIgnoreCase(status)) {
            settlementResult = auctionSettlementService.settleAuctionAndGetResult(auctionId);
            ok = settlementResult != null;
        } else {
            ok = adminDAO.updateAuctionStatus(auctionId, status);
        }

        if (ok) {
            auctionManager.reloadActiveAuctions();
            if (settlementResult != null) {
                ServerMain.publishSettlementResults(List.of(settlementResult));
            } else {
                ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Admin da cap nhat phien dau gia", null));
            }
        }
        sendResponse(ok
                ? ResponsePayload.success("ADMIN_UPDATE_AUCTION_STATUS_RESPONSE", "Da cap nhat phien dau gia.", adminDAO.getAuctions())
                : ResponsePayload.fail("ADMIN_UPDATE_AUCTION_STATUS_RESPONSE", "Khong the cap nhat phien dau gia."));
    }

    private void processAddBalance(String dataJson) {
        sendResponse(ResponsePayload.fail(
                "ADD_BALANCE_RESPONSE",
                "Nap tien truc tiep da bi tat. Vui long gui yeu cau nap tien de admin duyet."
        ));
    }
    private void broadcastAuctionTimeIfChanged(Long auctionId) {
        try {
            AuctionDAO.AuctionBidState state = new AuctionDAO().getAuctionBidState(auctionId);

            if (state == null || state.endTime == null) {
                return;
            }

            com.google.gson.JsonObject data = new com.google.gson.JsonObject();
            data.addProperty("auctionId", auctionId);
            data.addProperty("newEndTime", state.endTime.toString());
            data.addProperty("serverTime", VietnamTime.now().toString());

            /*
             * Broadcast to all clients once.
             * Auction Main va Auction Room deu can cap nhat thoi gian.
             * Khong broadcast rieng room nua de tranh client trong room nhan event 2 lan.
             */
            ServerMain.broadcast(
                    ResponsePayload.success("AUCTION_TIME_EXTENDED", "Auction time updated", data)
            );

        } catch (Exception e) {
            System.err.println("Loi broadcast thoi gian auction:");
            e.printStackTrace();
        }
    }

    private void processGetBidHistory(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long auctionId = json.get("auctionId").getAsLong();

        List<BidTransaction> history = new AuctionDAO().getBidHistory(auctionId);

        List<BidRequest> result = new java.util.ArrayList<>();

        for (BidTransaction bid : history) {
            BidRequest dto = new BidRequest();
            dto.auctionId = auctionId;
            dto.bidderId = bid.getBidderId();
            dto.amount = bid.getAmount();
            dto.autoBid = bid.isAutoBid();
            result.add(dto);
        }

        sendResponse(ResponsePayload.success("GET_BID_HISTORY_RESPONSE", "Thanh cong", result));
    }
    private void processAddWatchlist(String dataJson) {
        try {
            JsonObject json = parseObject(dataJson);

            Long userId = json.get("userId").getAsLong();
            Long auctionId = json.get("auctionId").getAsLong();

            if (!requireSameUser(userId, "ADD_WATCHLIST_RESPONSE")) {
                return;
            }

            boolean ok = new AuctionDAO().addToWatchlist(userId, auctionId);

            if (ok) {
                sendResponse(ResponsePayload.success(
                        "ADD_WATCHLIST_RESPONSE",
                        "Da them vao watchlist",
                        auctionId
                ));
            } else {
                sendResponse(ResponsePayload.fail(
                        "ADD_WATCHLIST_RESPONSE",
                        "Khong the them vao watchlist"
                ));
            }

        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(
                    "ADD_WATCHLIST_RESPONSE",
                    "Du lieu watchlist khong hop le: " + e.getMessage()
            ));
        }
    }

    private void processRemoveWatchlist(String dataJson) {
        try {
            JsonObject json = parseObject(dataJson);

            Long userId = json.get("userId").getAsLong();
            Long auctionId = json.get("auctionId").getAsLong();

            if (!requireSameUser(userId, "REMOVE_WATCHLIST_RESPONSE")) {
                return;
            }

            boolean ok = new AuctionDAO().removeFromWatchlist(userId, auctionId);

            if (ok) {
                sendResponse(ResponsePayload.success(
                        "REMOVE_WATCHLIST_RESPONSE",
                        "Da xoa khoi watchlist",
                        auctionId
                ));
            } else {
                sendResponse(ResponsePayload.fail(
                        "REMOVE_WATCHLIST_RESPONSE",
                        "San pham khong co trong watchlist"
                ));
            }

        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(
                    "REMOVE_WATCHLIST_RESPONSE",
                    "Du lieu watchlist khong hop le: " + e.getMessage()
            ));
        }
    }

    private void processGetWatchlistIds(String dataJson) {
        try {
            JsonObject json = parseObject(dataJson);

            Long userId = json.get("userId").getAsLong();

            if (!requireSameUser(userId, "GET_WATCHLIST_IDS_RESPONSE")) {
                return;
            }

            List<Long> ids = new AuctionDAO().getWatchlistIds(userId);

            sendResponse(ResponsePayload.success(
                    "GET_WATCHLIST_IDS_RESPONSE",
                    "Thanh cong",
                    ids
            ));

        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(
                    "GET_WATCHLIST_IDS_RESPONSE",
                    "Khong the lay watchlist ids: " + e.getMessage()
            ));
        }
    }
}
