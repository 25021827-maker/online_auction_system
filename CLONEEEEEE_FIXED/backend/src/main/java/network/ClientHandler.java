package network;

import com.google.gson.*;
import dao.AdminDAO;
import dao.AuctionDAO;
import dto.*;
import model.Auction;
import model.BidTransaction;
import model.User;
import service.AuctionManager;
import service.UserService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler implements Runnable, ClientObserver {
    private static final long MIN_AUCTION_DURATION_MINUTES = 5;
    private final Socket clientSocket;
    private final AuctionSubject auctionSubject;
    private final AuctionManager auctionManager;
    private final UserService userService;
    private final AdminDAO adminDAO;
    private final Gson gson;
    private PrintWriter out;
    private BufferedReader in;
    private volatile User authenticatedUser;

    public ClientHandler(Socket socket, AuctionSubject subject, AuctionManager manager) {
        this.clientSocket = socket;
        this.auctionSubject = subject;
        this.auctionManager = manager;
        this.userService = new UserService();
        this.adminDAO = new AdminDAO();
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
            auctionSubject.addObserver(this);

            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                RequestPayload request = gson.fromJson(requestLine, RequestPayload.class);
                if (request != null && request.getAction() != null) {
                    handleRequest(request);
                }
            }
        } catch (Exception e) {
            System.out.println("Client ngáº¯t káº¿t ná»‘i: " + e.getMessage());
        } finally {
            ServerMain.activeClients.remove(this);
            AuctionRoomManager.removeClientFromAllRooms(this);
            auctionSubject.removeObserver(this);
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void sendRealtimeUpdate(String message) {
        sendMessage(message);
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
                case "SUBMIT_DEPOSIT" -> processSubmitDeposit(request.getData());
                case "ADD_BALANCE" -> processAddBalance(request.getData());
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
                case "ADMIN_UPDATE_AUCTION_STATUS" -> processAdminUpdateAuctionStatus(request.getData());
                default -> sendResponse(ResponsePayload.fail(request.getAction() + "_RESPONSE", "HÃ nh Ä‘á»™ng khÃ´ng há»£p lá»‡: " + request.getAction()));
            }
        } catch (Exception e) {
            sendResponse(ResponsePayload.fail(request.getAction() + "_RESPONSE", "Lá»—i Server: " + e.getMessage()));
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
        sendResponse(loggedInUser != null
                ? ResponsePayload.success("LOGIN_RESPONSE", "ÄÄƒng nháº­p thÃ nh cÃ´ng", loggedInUser)
                : ResponsePayload.fail("LOGIN_RESPONSE", "Sai tÃªn Ä‘Äƒng nháº­p hoáº·c máº­t kháº©u"));
    }

    private void processRegister(String jsonData) {
        RegisterRequest req = gson.fromJson(jsonData, RegisterRequest.class);
        boolean isSuccess = userService.register(req.username, req.password, req.email, req.role);
        sendResponse(isSuccess
                ? ResponsePayload.success("REGISTER_RESPONSE", "ÄÄƒng kÃ½ thÃ nh cÃ´ng!", null)
                : ResponsePayload.fail("REGISTER_RESPONSE", "ÄÄƒng kÃ½ tháº¥t báº¡i. TÃªn Ä‘Äƒng nháº­p/email cÃ³ thá»ƒ Ä‘Ã£ tá»“n táº¡i."));
    }

    private void processBid(String jsonData) {
        BidRequest bidReq = gson.fromJson(jsonData, BidRequest.class);
        try {
            if (bidReq == null || !requireSameUser(bidReq.bidderId, "PLACE_BID_RESPONSE")) {
                return;
            }
            BidTransaction newBid = new BidTransaction(null, bidReq.bidderId, bidReq.amount);
            List<BidRequest> autoBids = auctionManager.processBidWithAutoBids(bidReq.auctionId, newBid);
            bidReq.autoBid = false;
            if (autoBids == null) {
                sendResponse(ResponsePayload.fail("PLACE_BID_RESPONSE", "GiÃ¡ Ä‘áº·t khÃ´ng há»£p lá»‡ hoáº·c phiÃªn Ä‘Ã£ Ä‘Ã³ng."));
                return;
            }

            broadcastBidEvent(bidReq, "New bid placed");
            for (BidRequest autoBid : autoBids) {
                broadcastBidEvent(autoBid, "Auto bid da dat gia moi");
            }
            sendResponse(ResponsePayload.success("PLACE_BID_RESPONSE", "Äáº·t giÃ¡ thÃ nh cÃ´ng!", null));
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

            for (BidRequest autoBid : auctionManager.processAutoBids(req.auctionId)) {
                broadcastBidEvent(autoBid, "Auto bid da dat gia moi");
            }
        } catch (exception.InvalidBidException | exception.AuctionClosedException e) {
            sendResponse(ResponsePayload.fail("SET_AUTO_BID_RESPONSE", e.getMessage()));
        }
    }

    private void broadcastBidEvent(BidRequest bid, String message) {
        ResponsePayload event = ResponsePayload.success("BID_UPDATE", message, bid);
        AuctionRoomManager.broadcastToRoom(bid.auctionId, gson.toJson(event));
    }

    private void processGetActiveAuctions() {
        List<Auction> activeAuctions = auctionManager.getActiveAuctionsList();
        sendResponse(ResponsePayload.success("GET_ACTIVE_AUCTIONS_RESPONSE", "ThÃ nh cÃ´ng", activeAuctions));
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
            boolean isSuccess = auctionManager.createNewAuction(req.sellerId, req.itemName, req.description,
                    req.startingPrice, req.category, req.condition, req.imagePath, start, end);

            if (isSuccess) {
                sendResponse(ResponsePayload.success("CREATE_AUCTION_RESPONSE", "Táº¡o phiÃªn Ä‘áº¥u giÃ¡ thÃ nh cÃ´ng", null));
                ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "CÃ³ sáº£n pháº©m má»›i", null));
            } else {
                sendResponse(ResponsePayload.fail("CREATE_AUCTION_RESPONSE", "KhÃ´ng thá»ƒ táº¡o phiÃªn Ä‘áº¥u giÃ¡."));
            }
        } catch (Exception e) {
            sendResponse(ResponsePayload.fail("CREATE_AUCTION_RESPONSE", "Dá»¯ liá»‡u táº¡o phiÃªn khÃ´ng há»£p lá»‡: " + e.getMessage()));
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
            boolean isSuccess = new AuctionDAO().updateAuction(req.auctionId, req.sellerId, req.itemName, req.description,
                    req.startingPrice, req.category, req.condition, start, end);

            if (isSuccess) {
                sendResponse(ResponsePayload.success("UPDATE_AUCTION_RESPONSE", "Cáº­p nháº­t thÃ nh cÃ´ng!", null));
                ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Sáº£n pháº©m Ä‘Æ°á»£c cáº­p nháº­t", null));
            } else {
                sendResponse(ResponsePayload.fail("UPDATE_AUCTION_RESPONSE", "KhÃ´ng thá»ƒ cáº­p nháº­t phiÃªn Ä‘áº¥u giÃ¡."));
            }
        } catch (Exception e) {
            sendResponse(ResponsePayload.fail("UPDATE_AUCTION_RESPONSE", "Dá»¯ liá»‡u cáº­p nháº­t khÃ´ng há»£p lá»‡: " + e.getMessage()));
        }
    }

    private void validateAuctionSchedule(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thoi gian dau gia khong hop le.");
        }
        LocalDateTime now = LocalDateTime.now();
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
        sendResponse(ResponsePayload.success("GET_MY_PRODUCTS_RESPONSE", "ThÃ nh cÃ´ng", new AuctionDAO().getAuctionsBySeller(sellerId)));
    }

    private void processDeleteProduct(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long auctionId = json.get("auctionId").getAsLong();
        Long sellerId = authenticatedUser != null ? authenticatedUser.getId() : null;
        if (!requireSeller(sellerId, "DELETE_PRODUCT_RESPONSE")) {
            return;
        }
        if (new AuctionDAO().deleteAuction(auctionId, sellerId)) {
            sendResponse(ResponsePayload.success("DELETE_PRODUCT_RESPONSE", "ÄÃ£ xÃ³a", null));
            ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Sáº£n pháº©m bá»‹ xÃ³a", null));
        } else {
            sendResponse(ResponsePayload.fail("DELETE_PRODUCT_RESPONSE", "KhÃ´ng thá»ƒ xÃ³a sáº£n pháº©m."));
        }
    }

    private void processGetWatchlist(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long userId = json.get("userId").getAsLong();
        if (!requireSameUser(userId, "GET_WATCHLIST_RESPONSE")) {
            return;
        }
        sendResponse(ResponsePayload.success("GET_WATCHLIST_RESPONSE", "ThÃ nh cÃ´ng", new AuctionDAO().getWatchlist(userId)));
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
        }
        sendResponse(ok
                ? ResponsePayload.success(action, "Da xu ly yeu cau nap tien.", adminDAO.getDepositRequests())
                : ResponsePayload.fail(action, "Khong the xu ly yeu cau nap tien."));
    }

    private void sendBalanceUpdate(Long userId) {
        Double balance = adminDAO.getUserBalance(userId);
        if (balance == null) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        data.addProperty("balance", balance);
        ServerMain.sendToUser(userId, ResponsePayload.success("BALANCE_UPDATE", "Balance updated.", data));
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
        boolean ok = adminDAO.updateAuctionStatus(auctionId, status);
        if (ok) {
            auctionManager.reloadActiveAuctions();
            ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "Admin da cap nhat phien dau gia", null));
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

    private void processGetBidHistory(String dataJson) {
        JsonObject json = parseObject(dataJson);
        Long auctionId = json.get("auctionId").getAsLong();
        List<BidTransaction> history = new AuctionDAO().getBidHistory(auctionId);
        sendResponse(ResponsePayload.success("GET_BID_HISTORY_RESPONSE", "ThÃ nh cÃ´ng", history));
    }
}

