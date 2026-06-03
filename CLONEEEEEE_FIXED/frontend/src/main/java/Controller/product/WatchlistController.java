package Controller.product;

import Model.Product;
import Session.Session;
import ui.product.ProductCard;
import network.SocketClient;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.AuctionDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;
import Service.core.SceneNavigator;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatchlistController {

    @FXML private FlowPane watchlistContainer;
    private Timeline liveTimeline;
    private final List<ProductCard> activeCards = new ArrayList<>();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        if (watchlistContainer != null) {
            watchlistContainer.setPrefWidth(1160.0);
        }

        // Đăng ký nhận danh sách Watchlist từ Server
        SocketClient.getInstance().on("GET_WATCHLIST_RESPONSE", this::handleLoadWatchlist);

        // Phát lệnh lấy danh sách Watchlist
        fetchWatchlistFromServer();

        // Chạy đồng hồ lùi giây liên tục
        liveTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            for (ProductCard card : activeCards) { card.update(); }
        }));
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    private void fetchWatchlistFromServer() {
        if (Session.currentUser == null) return;
        RequestPayload req = new RequestPayload("GET_WATCHLIST", "{\"userId\":" + Session.currentUser.getId() + "}");
        SocketClient.getInstance().sendRequest(req);
    }

    private void handleLoadWatchlist(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    for (ProductCard card : activeCards) { card.stopTimeline(); }
                    activeCards.clear();
                    watchlistContainer.getChildren().clear();

                    Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
                    List<AuctionDTO> dtos = gson.fromJson(gson.toJson(response.getData()), listType);

                    if (dtos != null) {
                        for (AuctionDTO dto : dtos) {
                            Product p = mapToProduct(dto);
                            ProductCard card = new ProductCard(p);
                            activeCards.add(card);
                            watchlistContainer.getChildren().add(card.getRoot());
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    private Product mapToProduct(AuctionDTO dto) {
        String title = (dto.item != null) ? dto.item.name : "";
        double price = dto.currentPrice;
        String image = (dto.item != null) ? dto.item.imagePath : "";
        String desc = (dto.item != null) ? dto.item.description : "";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime st = dto.startTime != null ? LocalDateTime.parse(dto.startTime, formatter) : LocalDateTime.now();
        LocalDateTime et = dto.endTime != null ? LocalDateTime.parse(dto.endTime, formatter) : LocalDateTime.now();

        Product p = new Product(title, price, image, "Seller#" + dto.sellerId, st, et, desc);
        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, dto.id.intValue());
        } catch (Exception e) {}
        p.setStatus(dto.status);
        return p;
    }

    @FXML
    private void goBack() {
        if (liveTimeline != null) liveTimeline.stop();
        for (ProductCard card : activeCards) card.stopTimeline();
        try { SceneNavigator.loadFromNode(watchlistContainer, "/ui/product/AuctionMain.fxml", "Sàn đấu giá"); } catch (Exception e) {}
    }
}