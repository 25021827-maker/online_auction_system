package network;

import java.util.ArrayList;
import java.util.List;

public class AuctionSubject {
    private final List<ClientObserver> observers = new ArrayList<>();

    public synchronized void addObserver(ClientObserver obs) { observers.add(obs); }
    public synchronized void removeObserver(ClientObserver obs) { observers.remove(obs); }
    public synchronized void notifyAllClients(String message) {
        for (ClientObserver obs : observers) {
            obs.sendRealtimeUpdate(message);
        }
    }
}