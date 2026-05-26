package client;

import com.google.gson.Gson;
import dto.RequestPayload;
import dto.ResponsePayload;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class AuctionClient {
    private static AuctionClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private final Map<String, CompletableFuture<ResponsePayload>> pendingRequests = new ConcurrentHashMap<>();
    private Consumer<ResponsePayload> broadcastListener;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean connected = false;          // ← Thêm biến trạng thái kết nối

    private AuctionClient() {}

    public static AuctionClient getInstance() {
        if (instance == null) instance = new AuctionClient();
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        startListening();
        connected = true;                       // ← Đánh dấu đã kết nối
    }

    public boolean isConnected() {
        return connected;
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    ResponsePayload resp = gson.fromJson(line, ResponsePayload.class);
                    if (resp.getCorrelationId() != null) {
                        CompletableFuture<ResponsePayload> future = pendingRequests.remove(resp.getCorrelationId());
                        if (future != null) future.complete(resp);
                    } else if (broadcastListener != null) {
                        final ResponsePayload broadcast = resp;
                        Platform.runLater(() -> broadcastListener.accept(broadcast));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                connected = false;              // Mất kết nối
            }
        }).start();
    }

    public CompletableFuture<ResponsePayload> sendRequest(String action, Object data) {
        if (!connected) {
            CompletableFuture<ResponsePayload> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IOException("Not connected to server"));
            return failed;
        }
        String correlationId = UUID.randomUUID().toString();
        RequestPayload req = new RequestPayload(action, data);
        req.setCorrelationId(correlationId);
        CompletableFuture<ResponsePayload> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);
        out.println(gson.toJson(req));

        scheduler.schedule(() -> {
            CompletableFuture<ResponsePayload> f = pendingRequests.remove(correlationId);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(new TimeoutException("Request timeout after 10 seconds"));
            }
        }, 10, TimeUnit.SECONDS);

        return future;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connected = false;
            scheduler.shutdown();
        }
    }

    public void setOnBroadcast(Consumer<ResponsePayload> listener) {
        this.broadcastListener = listener;
    }
}