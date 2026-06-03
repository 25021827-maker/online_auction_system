package network;

import com.google.gson.*;
import dto.RequestPayload;
import dto.ResponsePayload;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;

    // ĐÃ SỬA: Hỗ trợ nhiều Controller cùng lắng nghe 1 sự kiện (List<Consumer>)
    private final ConcurrentHashMap<String, List<Consumer<ResponsePayload>>> eventListeners;

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8080;

    private SocketClient() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                        (json, type, context) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                        (src, type, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();

        this.eventListeners = new ConcurrentHashMap<>();
        connectToServer();
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("✅ [SocketClient] Đã kết nối Backend!");

            Thread listenerThread = new Thread(this::listenForServerMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();
        } catch (Exception e) {
            System.err.println("❌ [SocketClient] Không thể kết nối tới Server: " + e.getMessage());
        }
    }

    // ĐÃ SỬA: Thêm người nghe vào danh sách thay vì ghi đè
    public void on(String eventName, Consumer<ResponsePayload> callback) {
        eventListeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void sendRequest(RequestPayload request) {
        if (out != null) {
            out.println(gson.toJson(request));
        }
    }

    private void listenForServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                ResponsePayload response = gson.fromJson(serverMessage, ResponsePayload.class);
                String action = response.getAction();

                if (action != null && eventListeners.containsKey(action)) {
                    // ĐÃ SỬA: Bắn dữ liệu cho toàn bộ các Controller đang đăng ký nghe
                    List<Consumer<ResponsePayload>> callbacks = eventListeners.get(action);
                    for (Consumer<ResponsePayload> callback : callbacks) {
                        Platform.runLater(() -> callback.accept(response));
                    }
                }
            }
        } catch (Exception e) {}
    }
}