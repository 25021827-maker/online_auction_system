package network;

import java.util.ArrayList;
import java.util.List;

public class AuctionSubject {

    private final List<ClientObserver> observers = new ArrayList<>();


    public synchronized void addObserver(ClientObserver observer) {
        observers.add(observer);
    }


    public synchronized void removeObserver(ClientObserver observer) {
        observers.remove(observer);
    }


    public void notifyAllClients(String message) {
        List<ClientObserver> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(observers);
        }
        for (ClientObserver observer : snapshot) {
            observer.sendRealtimeUpdate(message);
        }
    }
}
