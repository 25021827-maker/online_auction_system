package util;

import dto.BidResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventBus {
    private static EventBus instance;
    private List<Consumer<Double>> balanceListeners = new ArrayList<>();
    private List<Consumer<BidResult>> auctionUpdateListeners = new ArrayList<>();

    private EventBus() {}

    public static EventBus getInstance() {
        if (instance == null) instance = new EventBus();
        return instance;
    }

    // Balance listeners
    public void addBalanceListener(Consumer<Double> listener) {
        balanceListeners.add(listener);
    }

    public void removeBalanceListener(Consumer<Double> listener) {
        balanceListeners.remove(listener);
    }

    public void fireBalanceChanged(double newBalance) {
        balanceListeners.forEach(l -> l.accept(newBalance));
    }

    // Auction update listeners (cập nhật giá realtime)
    public void addAuctionUpdateListener(Consumer<BidResult> listener) {
        auctionUpdateListeners.add(listener);
    }

    public void removeAuctionUpdateListener(Consumer<BidResult> listener) {
        auctionUpdateListeners.remove(listener);
    }

    public void fireAuctionUpdate(BidResult result) {
        auctionUpdateListeners.forEach(l -> l.accept(result));
    }
}