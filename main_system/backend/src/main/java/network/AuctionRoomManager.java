package network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AuctionRoomManager {
    private static final ConcurrentHashMap<Long, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();

    private AuctionRoomManager() {
    }

    public static void joinRoom(Long auctionId, ClientHandler client) {
        if (auctionId == null || client == null) {
            return;
        }
        rooms.computeIfAbsent(auctionId, id -> ConcurrentHashMap.newKeySet()).add(client);
    }

    public static void leaveRoom(Long auctionId, ClientHandler client) {
        if (auctionId == null || client == null) {
            return;
        }

        Set<ClientHandler> clients = rooms.get(auctionId);
        if (clients == null) {
            return;
        }

        clients.remove(client);
        if (clients.isEmpty()) {
            rooms.remove(auctionId, clients);
        }
    }

    public static void broadcastToRoom(Long auctionId, String message) {
        Set<ClientHandler> clients = rooms.get(auctionId);
        if (clients == null) {
            return;
        }

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void removeClientFromAllRooms(ClientHandler client) {
        if (client == null) {
            return;
        }

        for (Set<ClientHandler> clients : rooms.values()) {
            clients.remove(client);
        }
        rooms.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
