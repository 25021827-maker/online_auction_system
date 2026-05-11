package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.UserDAO;
import dto.RequestPayload;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable, ClientObserver {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private AuctionSubject auctionSubject;

    private UserDAO userDAO;

    public ClientHandler(Socket socket, AuctionSubject subject) {
        this.clientSocket = socket;
        this.auctionSubject = subject;
        this.gson = new Gson();
        this.userDAO = new UserDAO();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            auctionSubject.addObserver(this);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                RequestPayload request = gson.fromJson(inputLine, RequestPayload.class);
                handleRequest(request);
            }
        } catch (Exception e) {
            System.out.println("Client ngắt kết nối: " + clientSocket.getInetAddress());
        } finally {
            auctionSubject.removeObserver(this);
            try { clientSocket.close(); } catch (Exception e) {}
        }
    }

    private void handleRequest(RequestPayload request) {
        JsonObject dataObj = request.getData() != null ? gson.fromJson(request.getData(), JsonObject.class) : null;

        switch (request.getAction()) {
            case "LOGIN":
                if (dataObj != null) {
                    String user = dataObj.get("username").getAsString();
                    String pass = dataObj.get("password").getAsString();

                    if (userDAO.authenticateUser(user, pass)) {
                        out.println("{\"status\": \"SUCCESS\", \"message\": \"Đăng nhập thành công\"}");
                    } else {
                        out.println("{\"status\": \"FAILED\", \"message\": \"Sai tài khoản hoặc mật khẩu\"}");
                    }
                }
                break;

            case "REGISTER":
                if (dataObj != null) {
                    String id = dataObj.get("id").getAsString();
                    String user = dataObj.get("username").getAsString();
                    String pass = dataObj.get("password").getAsString();
                    String role = dataObj.get("role").getAsString();

                    if (userDAO.registerUser(id, user, pass, role)) {
                        out.println("{\"status\": \"SUCCESS\"}");
                    } else {
                        out.println("{\"status\": \"FAILED\", \"message\": \"Tên đăng nhập đã tồn tại!\"}");
                    }
                }
                break;

            case "PLACE_BID":
                if (dataObj != null) {
                    String bidder = dataObj.get("bidderId").getAsString();
                    double amount = dataObj.get("amount").getAsDouble();
                    String updateMsg = String.format("{\"event\": \"NEW_BID\", \"bidder\": \"%s\", \"amount\": %s}", bidder, amount);
                    auctionSubject.notifyAllClients(updateMsg);
                }
                break;

            default:
                out.println("{\"status\": \"ERROR\", \"message\": \"Hành động không hợp lệ\"}");
        }
    }

    @Override
    public void sendRealtimeUpdate(String jsonData) {
        if (out != null) {
            out.println(jsonData);
        }
    }
}