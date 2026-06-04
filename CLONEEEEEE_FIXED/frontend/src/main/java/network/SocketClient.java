package network;

import com.google.gson.*;
import Session.Session;
import dto.RequestPayload;
import dto.ResponsePayload;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;

    // Supports multiple controllers listening to the same server event.
    private final ConcurrentHashMap<String, List<Consumer<ResponsePayload>>> eventListeners;

    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8080;

    private final String serverHost;
    private final int serverPort;

    private SocketClient() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                        (json, type, context) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                        (src, type, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();

        this.eventListeners = new ConcurrentHashMap<>();
        Properties clientProperties = loadClientProperties();
        this.serverHost = loadServerHost(clientProperties);
        this.serverPort = loadServerPort(clientProperties);
        connectToServer();
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    private Properties loadClientProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/client.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            System.err.println("[SocketClient] Cannot load client.properties: " + e.getMessage());
        }
        return props;
    }

    private String loadServerHost(Properties props) {
        String value = System.getProperty("server.host");
        if (value == null || value.isBlank()) {
            value = System.getenv("AUCTION_SERVER_HOST");
        }
        if (value == null || value.isBlank()) {
            value = props.getProperty("server.host");
        }
        return (value == null || value.isBlank()) ? DEFAULT_SERVER_HOST : value.trim();
    }

    private int loadServerPort(Properties props) {
        String value = System.getProperty("server.port");
        if (value == null || value.isBlank()) {
            value = System.getenv("AUCTION_SERVER_PORT");
        }
        if (value == null || value.isBlank()) {
            value = props.getProperty("server.port");
        }
        if (value == null || value.isBlank()) {
            return DEFAULT_SERVER_PORT;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_SERVER_PORT;
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[SocketClient] Connected to Backend " + serverHost + ":" + serverPort);

            Thread listenerThread = new Thread(this::listenForServerMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();
        } catch (Exception e) {
            System.err.println("[SocketClient] Cannot connect to Backend " + serverHost + ":" + serverPort + " - " + e.getMessage());
        }
    }

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
                updateSessionBalanceIfNeeded(response);

                if (action != null && eventListeners.containsKey(action)) {
                    List<Consumer<ResponsePayload>> callbacks = eventListeners.get(action);
                    for (Consumer<ResponsePayload> callback : callbacks) {
                        Platform.runLater(() -> callback.accept(response));
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void updateSessionBalanceIfNeeded(ResponsePayload response) {
        if (!"BALANCE_UPDATE".equals(response.getAction()) || Session.currentUser == null) {
            return;
        }

        try {
            JsonObject data = JsonParser.parseString(gson.toJson(response.getData())).getAsJsonObject();
            long userId = data.get("userId").getAsLong();
            double balance = data.get("balance").getAsDouble();
            if (Session.currentUser.getId() != null && Session.currentUser.getId().equals(userId)) {
                Session.currentUser.setBalance(balance);
            }
        } catch (Exception ignored) {}
    }
}
