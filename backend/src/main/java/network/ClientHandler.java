package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.AuctionDTO;
import service.AuctionService;
import controller.AuthController;
import dao.UserDAO;
import java.io.*;
import java.net.Socket;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable, ClientObserver {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private AuctionSubject auctionSubject;
    private AuctionService auctionService;
    private AuthController authController;
    private UserDAO userDAO;
    private Long currentUserId;

    public ClientHandler(Socket socket, AuctionSubject subject, AuctionService service) {
        this.socket = socket;
        this.auctionSubject = subject;
        this.auctionService = service;
        this.authController = new AuthController();
        this.userDAO = new UserDAO();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            auctionSubject.addObserver(this);

            String line;
            while ((line = in.readLine()) != null) {
                RequestPayload req = gson.fromJson(line, RequestPayload.class);
                handleRequest(req);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected");
        } finally {
            auctionSubject.removeObserver(this);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // ==================== Helper kiểm tra phân quyền ====================
    private boolean isSeller() throws Exception {
        if (currentUserId == null) return false;
        String role = userDAO.getUserRole(currentUserId);
        return "SELLER".equals(role) || "ADMIN".equals(role);
    }

    private boolean isAdmin() throws Exception {
        if (currentUserId == null) return false;
        String role = userDAO.getUserRole(currentUserId);
        return "ADMIN".equals(role);
    }

    private void handleRequest(RequestPayload req) {
        String correlationId = req.getCorrelationId();
        ResponsePayload resp;
        try {
            switch (req.getAction()) {
                case "LOGIN":
                    JsonObject loginData = gson.fromJson(req.getData(), JsonObject.class);
                    String username = loginData.get("username").getAsString();
                    String password = loginData.get("password").getAsString();
                    String loginResp = authController.handleLogin(username, password);
                    JsonObject loginJson = gson.fromJson(loginResp, JsonObject.class);
                    if ("SUCCESS".equals(loginJson.get("status").getAsString())) {
                        long id = loginJson.getAsJsonObject("user").get("id").getAsLong();
                        this.currentUserId = id;
                    }
                    resp = new ResponsePayload(
                            loginJson.get("status").getAsString(),
                            loginJson.get("user")
                    );
                    break;

                case "REGISTER":
                    JsonObject regData = gson.fromJson(req.getData(), JsonObject.class);
                    String regUser = regData.get("username").getAsString();
                    String regPass = regData.get("password").getAsString();
                    String regEmail = regData.has("email") ? regData.get("email").getAsString() : null;
                    String regRole = regData.get("role").getAsString();
                    String regResp = authController.handleRegister(regUser, regPass, regEmail, regRole);
                    ResponsePayload regPayload = gson.fromJson(regResp, ResponsePayload.class);
                    resp = regPayload;
                    break;

                case "ADD_MONEY":
                    if (currentUserId == null) throw new RuntimeException("Chưa đăng nhập");
                    JsonObject moneyData = gson.fromJson(req.getData(), JsonObject.class);
                    double amount = moneyData.get("amount").getAsDouble();
                    boolean success = userDAO.addBalance(currentUserId, amount);
                    if (success) {
                        resp = new ResponsePayload("SUCCESS", Map.of("message", "Nạp tiền thành công"));
                    } else {
                        resp = new ResponsePayload("ERROR", "Nạp tiền thất bại");
                    }
                    break;

                case "GET_BALANCE":
                    if (currentUserId == null) throw new RuntimeException("Chưa đăng nhập");
                    double balance = userDAO.getBalance(currentUserId);
                    resp = new ResponsePayload("SUCCESS", Map.of("balance", balance));
                    break;

                case "PLACE_BID":
                    if (currentUserId == null) throw new RuntimeException("Chưa đăng nhập");
                    JsonObject bidData = gson.fromJson(req.getData(), JsonObject.class);
                    Long auctionId = bidData.get("auctionId").getAsLong();
                    BigDecimal bidAmount = BigDecimal.valueOf(bidData.get("amount").getAsDouble());

                    String bidResultJson = auctionService.placeBid(currentUserId, auctionId, bidAmount);
                    JsonObject bidJson = gson.fromJson(bidResultJson, JsonObject.class);
                    String status = bidJson.get("status").getAsString();
                    Object messageOrData = bidJson.get("message");
                    if (messageOrData == null && bidJson.has("currentPrice")) {
                        messageOrData = bidJson;
                    }
                    resp = new ResponsePayload(status, messageOrData);

                    if ("SUCCESS".equals(status)) {
                        auctionSubject.notifyAllClients(bidResultJson);
                    }
                    break;

                case "GET_ACTIVE_AUCTIONS":
                    List<AuctionDTO> activeDTOs = auctionService.getActiveAuctionsDTO();
                    resp = new ResponsePayload("SUCCESS", activeDTOs);
                    break;

                case "CREATE_AUCTION":
                    if (currentUserId == null) throw new RuntimeException("Chưa đăng nhập");
                    if (!isSeller()) {
                        resp = new ResponsePayload("ERROR", "Chỉ seller mới được tạo đấu giá");
                        break;
                    }
                    JsonObject createData = gson.fromJson(req.getData(), JsonObject.class);
                    String name = createData.get("name").getAsString();
                    String description = createData.get("description").getAsString();
                    BigDecimal startPrice = BigDecimal.valueOf(createData.get("startingPrice").getAsDouble());
                    LocalDateTime startTime = LocalDateTime.parse(createData.get("startTime").getAsString());
                    LocalDateTime endTime = LocalDateTime.parse(createData.get("endTime").getAsString());
                    BigDecimal minStep = BigDecimal.valueOf(createData.get("minBidStep").getAsDouble());
                    String category = createData.get("category").getAsString();
                    AuctionDTO created = auctionService.createAuction(currentUserId, name, description, startPrice, startTime, endTime, minStep, category);
                    resp = new ResponsePayload("SUCCESS", created);
                    break;

                case "FILTER_AUCTIONS":
                    JsonObject filterData = gson.fromJson(req.getData(), JsonObject.class);
                    String categoryFilter = filterData.has("category") ? filterData.get("category").getAsString() : null;
                    Double minPrice = filterData.has("minPrice") ? filterData.get("minPrice").getAsDouble() : null;
                    Double maxPrice = filterData.has("maxPrice") ? filterData.get("maxPrice").getAsDouble() : null;
                    String statusFilter = filterData.has("status") ? filterData.get("status").getAsString() : null;
                    List<AuctionDTO> filtered = auctionService.filterAuctions(categoryFilter, minPrice, maxPrice, statusFilter);
                    resp = new ResponsePayload("SUCCESS", filtered);
                    break;

                case "GET_MY_AUCTIONS":
                    if (currentUserId == null) throw new RuntimeException("Chưa đăng nhập");
                    if (!isSeller()) {
                        resp = new ResponsePayload("ERROR", "Chỉ seller mới xem được các phiên của mình");
                        break;
                    }
                    List<AuctionDTO> myAuctions = auctionService.getAuctionsBySeller(currentUserId);
                    resp = new ResponsePayload("SUCCESS", myAuctions);
                    break;

                default:
                    resp = new ResponsePayload("ERROR", "Unknown action");
            }
        } catch (Exception e) {
            resp = new ResponsePayload("ERROR", e.getMessage());
        }
        resp.setCorrelationId(correlationId);
        out.println(gson.toJson(resp));
    }

    @Override
    public void sendRealtimeUpdate(String jsonData) {
        out.println(jsonData);
    }
}