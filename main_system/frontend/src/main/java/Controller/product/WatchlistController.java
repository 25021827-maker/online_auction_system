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
import util.VietnamTime;

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

        SocketClient socketClient = SocketClient.getInstance();

        socketClient.clearListeners("GET_WATCHLIST_RESPONSE");
        socketClient.clearListeners("NEW_BID_EVENT");
        socketClient.clearListeners("AUCTION_PRICE_CHANGED");
        socketClient.clearListeners("AUCTION_TIME_EXTENDED");
        socketClient.clearListeners("NEW_AUCTION_EVENT");
        socketClient.clearListeners("ADD_WATCHLIST_RESPONSE");
        socketClient.clearListeners("REMOVE_WATCHLIST_RESPONSE");

        socketClient.on("GET_WATCHLIST_RESPONSE", this::handleLoadWatchlist);
        socketClient.on("NEW_BID_EVENT", response -> fetchWatchlistFromServer());
        socketClient.on("AUCTION_PRICE_CHANGED", response -> fetchWatchlistFromServer());
        socketClient.on("AUCTION_TIME_EXTENDED", response -> fetchWatchlistFromServer());
        socketClient.on("NEW_AUCTION_EVENT", response -> fetchWatchlistFromServer());

        /*
         * Khi user bấm Watch/Unwatch ngay trong màn Watchlist,
         * reload lại danh sách để nếu đã Unwatch thì card biến mất khỏi My Watchlist.
         */
        socketClient.on("ADD_WATCHLIST_RESPONSE", response -> {
            if ("SUCCESS".equals(response.getStatus())) {
                fetchWatchlistFromServer();
            }
        });

        socketClient.on("REMOVE_WATCHLIST_RESPONSE", response -> {
            if ("SUCCESS".equals(response.getStatus())) {
                fetchWatchlistFromServer();
            }
        });

        fetchWatchlistFromServer();

        liveTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            for (ProductCard card : activeCards) {
                card.update();
            }
        }));
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    private void fetchWatchlistFromServer() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        RequestPayload req = new RequestPayload(
                "GET_WATCHLIST",
                "{\"userId\":" + Session.getCurrentUser().getId() + "}"
        );

        SocketClient.getInstance().sendRequest(req);
    }

    private void handleLoadWatchlist(ResponsePayload response) {
        if (!"SUCCESS".equals(response.getStatus())) {
            return;
        }

        Platform.runLater(() -> {
            try {
                for (ProductCard card : activeCards) {
                    card.stopTimeline();
                }

                activeCards.clear();

                if (watchlistContainer != null) {
                    watchlistContainer.getChildren().clear();
                }

                Type listType = new TypeToken<List<AuctionDTO>>() {}.getType();
                List<AuctionDTO> dtos = gson.fromJson(gson.toJson(response.getData()), listType);

                /*
                 * Reset local watchlist theo đúng dữ liệu server.
                 * Sau đó mapToProduct sẽ add lại từng ID đang có trong Watchlist.
                 */
                if (Session.getCurrentUser() != null) {
                    Session.getCurrentUser().setWatchlistProductIds(new ArrayList<>());
                }

                if (dtos == null || dtos.isEmpty()) {
                    return;
                }

                for (AuctionDTO dto : dtos) {
                    Product p = mapToProduct(dto);
                    ProductCard card = new ProductCard(p);

                    activeCards.add(card);

                    if (watchlistContainer != null) {
                        watchlistContainer.getChildren().add(card.getRoot());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Product mapToProduct(AuctionDTO dto) {
        String title = (dto.item != null && dto.item.name != null)
                ? dto.item.name
                : "Untitled product";

        double price = dto.currentPrice > 0
                ? dto.currentPrice
                : (dto.item != null ? dto.item.startingPrice : 0);

        String image = (dto.item != null && dto.item.imagePath != null)
                ? dto.item.imagePath
                : "";

        String desc = (dto.item != null && dto.item.description != null)
                ? dto.item.description
                : "";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        LocalDateTime st = dto.startTime != null
                ? LocalDateTime.parse(dto.startTime, formatter)
                : VietnamTime.now();

        LocalDateTime et = dto.endTime != null
                ? LocalDateTime.parse(dto.endTime, formatter)
                : VietnamTime.now().plusHours(1);

        Product p = new Product(
                title,
                price,
                image,
                "Seller#" + dto.sellerId,
                st,
                et,
                desc
        );

        if (dto.serverTime != null && !dto.serverTime.isBlank()) {
            try {
                p.syncServerTime(
                        LocalDateTime.parse(dto.serverTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            } catch (Exception ignored) {
            }
        }

        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, dto.id.intValue());
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.setStatus(dto.status);

        if (dto.highestBidderId != null && dto.highestBidderId > 0) {
            p.setHighestBidder("User #" + dto.highestBidderId);
        } else {
            p.setHighestBidder("");
        }

        if (dto.item != null) {
            p.setCategory(dto.item.category);
            p.setCondition(dto.item.condition);
            p.setImageBase64(dto.item.imageBase64);
        }

        if (Session.getCurrentUser() != null) {
            Session.getCurrentUser().addToWatchlist(p.getId());
        }

        return p;
    }

    @FXML
    private void goBack() {
        if (liveTimeline != null) {
            liveTimeline.stop();
        }

        for (ProductCard card : activeCards) {
            card.stopTimeline();
        }

        try {
            SceneNavigator.loadFromNode(
                    watchlistContainer,
                    "/ui/product/AuctionMain.fxml",
                    "San dau gia"
            );
        } catch (Exception ignored) {
        }
    }
}