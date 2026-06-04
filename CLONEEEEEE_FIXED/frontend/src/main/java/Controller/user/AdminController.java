package Controller.user;

import javafx.scene.control.Alert;
import Service.core.SceneNavigator;
import Session.Session;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AdminAuctionDTO;
import dto.AdminDashboardDTO;
import dto.AdminDepositDTO;
import dto.AdminProductDTO;
import dto.AdminUserDTO;
import dto.AdminWinnerDTO;
import dto.RequestPayload;
import dto.ResponsePayload;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.SocketClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

public class AdminController {
    @FXML private Label lblTitle;
    @FXML private StackPane contentArea;

    private final Gson gson = new Gson();
    private String currentSection = "DASHBOARD";

    @FXML
    public void initialize() {
        SocketClient socketClient = SocketClient.getInstance();
        for (String action : List.of(
                "GET_ADMIN_DASHBOARD_RESPONSE",
                "GET_ADMIN_USERS_RESPONSE",
                "ADMIN_SET_USER_ACTIVE_RESPONSE",
                "GET_ADMIN_DEPOSITS_RESPONSE",
                "ADMIN_APPROVE_DEPOSIT_RESPONSE",
                "ADMIN_REJECT_DEPOSIT_RESPONSE",
                "GET_ADMIN_PRODUCTS_RESPONSE",
                "ADMIN_APPROVE_PRODUCT_RESPONSE",
                "ADMIN_REJECT_PRODUCT_RESPONSE",
                "GET_ADMIN_AUCTIONS_RESPONSE",
                "ADMIN_UPDATE_AUCTION_STATUS_RESPONSE",
                "GET_ADMIN_WINNERS_RESPONSE",
                "GET_ACTIVE_AUCTIONS_RESPONSE",
                "NEW_BID_EVENT",
                "NEW_AUCTION_EVENT"
        )) {
            socketClient.clearListeners(action);
        }
        socketClient.on("GET_ADMIN_DASHBOARD_RESPONSE", this::handleDashboardResponse);
        socketClient.on("GET_ADMIN_USERS_RESPONSE", this::handleUsersResponse);
        socketClient.on("ADMIN_SET_USER_ACTIVE_RESPONSE", this::handleUsersResponse);
        socketClient.on("GET_ADMIN_DEPOSITS_RESPONSE", this::handleDepositsResponse);
        socketClient.on("ADMIN_APPROVE_DEPOSIT_RESPONSE", this::handleDepositsResponse);
        socketClient.on("ADMIN_REJECT_DEPOSIT_RESPONSE", this::handleDepositsResponse);
        socketClient.on("GET_ADMIN_PRODUCTS_RESPONSE", this::handleProductsResponse);
        socketClient.on("ADMIN_APPROVE_PRODUCT_RESPONSE", this::handleProductsResponse);
        socketClient.on("ADMIN_REJECT_PRODUCT_RESPONSE", this::handleProductsResponse);
        socketClient.on("GET_ADMIN_AUCTIONS_RESPONSE", this::handleAuctionsResponse);
        socketClient.on("ADMIN_UPDATE_AUCTION_STATUS_RESPONSE", this::handleAuctionsResponse);
        socketClient.on("GET_ADMIN_WINNERS_RESPONSE", this::handleWinnersResponse);
        socketClient.on("NEW_BID_EVENT", this::handleBidCatalogChanged);
        socketClient.on("NEW_AUCTION_EVENT", this::handleAuctionCatalogChanged);
        switchDashboard();
    }

    @FXML
    private void switchDashboard() {
        currentSection = "DASHBOARD";
        lblTitle.setText("Admin Dashboard");
        showLoading("Loading dashboard...");
        send("GET_ADMIN_DASHBOARD", "{}");
    }

    @FXML
    private void switchDeposits() {
        currentSection = "DEPOSITS";
        lblTitle.setText("Approve Deposits");
        showLoading("Loading deposit requests...");
        send("GET_ADMIN_DEPOSITS", "{}");
    }

    @FXML
    private void switchProducts() {
        currentSection = "PRODUCTS";
        lblTitle.setText("Approve Products");
        showLoading("Loading product approvals...");
        send("GET_ADMIN_PRODUCTS", "{}");
    }

    @FXML
    private void switchUsers() {
        currentSection = "USERS";
        lblTitle.setText("Manage Users");
        showLoading("Loading users...");
        send("GET_ADMIN_USERS", "{}");
    }

    @FXML
    private void switchAuctions() {
        currentSection = "AUCTIONS";
        lblTitle.setText("Manage Auctions");
        showLoading("Loading auctions...");
        send("GET_ADMIN_AUCTIONS", "{}");
    }

    @FXML
    private void switchWinners() {
        currentSection = "WINNERS";
        lblTitle.setText("Auction Winners");
        showLoading("Loading auction winners...");
        send("GET_ADMIN_WINNERS", "{}");
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        SceneNavigator.loadFromNode(lblTitle, "/ui/auth/Login.fxml", "Login");
    }

    private void handleDashboardResponse(ResponsePayload response) {
        if (!isSuccess(response)) return;
        AdminDashboardDTO dashboard = gson.fromJson(gson.toJson(response.getData()), AdminDashboardDTO.class);
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.add(metric("Pending deposits", dashboard.pendingDeposits), 0, 0);
        grid.add(metric("Pending products", dashboard.pendingProducts), 1, 0);
        grid.add(metric("Total users", dashboard.totalUsers), 2, 0);
        grid.add(metric("Total auctions", dashboard.totalAuctions), 0, 1);
        grid.add(metric("Active auctions", dashboard.activeAuctions), 1, 1);
        grid.add(metric("User balance pool", "$" + String.format("%.2f", dashboard.totalUserBalance)), 2, 1);
        setContent(grid);
    }

    private void handleUsersResponse(ResponsePayload response) {
        if (!isSuccess(response)) return;
        Type type = new TypeToken<List<AdminUserDTO>>() {}.getType();
        List<AdminUserDTO> users = gson.fromJson(gson.toJson(response.getData()), type);

        TableView<AdminUserDTO> table = new TableView<>(FXCollections.observableArrayList(users));
        addColumn(table, "ID", u -> String.valueOf(u.userId), 70);
        addColumn(table, "Username", u -> u.username, 150);
        addColumn(table, "Email", u -> u.email, 220);
        addColumn(table, "Role", u -> u.role, 100);
        addColumn(table, "Balance", u -> "$" + String.format("%.2f", u.balance), 110);
        addColumn(table, "Available", u -> "$" + String.format("%.2f", u.availableBalance), 120);
        addColumn(table, "Status", u -> u.active ? "ACTIVE" : "DISABLED", 100);

        TableColumn<AdminUserDTO, Void> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(150);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button toggle = new Button();

            {
                toggle.getStyleClass().add("table-action-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AdminUserDTO user = getTableView().getItems().get(getIndex());
                toggle.setText(user.active ? "Disable" : "Enable");
                toggle.setDisable("admin".equalsIgnoreCase(user.username));
                toggle.setOnAction(e -> setUserActive(user.userId, !user.active));
                setGraphic(toggle);
            }
        });
        table.getColumns().add(actions);
        setContent(table);
    }

    private void handleWinnersResponse(ResponsePayload response) {
        if (!isSuccess(response)) return;
        Type type = new TypeToken<List<AdminWinnerDTO>>() {}.getType();
        List<AdminWinnerDTO> winners = gson.fromJson(gson.toJson(response.getData()), type);

        TableView<AdminWinnerDTO> table = new TableView<>(FXCollections.observableArrayList(winners));
        addColumn(table, "Auction", w -> String.valueOf(w.auctionId), 80);
        addColumn(table, "Product", w -> w.itemName, 240);
        addColumn(table, "Seller", w -> w.sellerUsername, 150);
        addColumn(table, "Winner", w -> w.winnerUsername, 150);
        addColumn(table, "Winning Price", w -> "$" + String.format("%.2f", w.finalPrice), 140);
        addColumn(table, "Payment", w -> w.status, 100);
        addColumn(table, "Settled At", w -> shorten(w.createdAt), 170);
        setContent(table);
    }

    private void handleDepositsResponse(ResponsePayload response) {
        if (!isSuccess(response)) return;
        Type type = new TypeToken<List<AdminDepositDTO>>() {}.getType();
        List<AdminDepositDTO> deposits = gson.fromJson(gson.toJson(response.getData()), type);

        TableView<AdminDepositDTO> table = new TableView<>(FXCollections.observableArrayList(deposits));
        addColumn(table, "Req ID", d -> String.valueOf(d.requestId), 80);
        addColumn(table, "User", d -> d.username, 150);
        addColumn(table, "Amount", d -> "$" + String.format("%.2f", d.amount), 110);
        addColumn(table, "Proof", d -> d.proofImagePath, 220);
        addColumn(table, "Status", d -> d.status, 110);
        addColumn(table, "Created", d -> shorten(d.createdAt), 170);

        TableColumn<AdminDepositDTO, Void> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(180);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button approve = new Button("Approve");
            private final Button reject = new Button("Reject");
            private final HBox box = new HBox(8, approve, reject);

            {
                approve.getStyleClass().addAll("table-action-button", "success-button");
                reject.getStyleClass().addAll("table-action-button", "danger-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AdminDepositDTO deposit = getTableView().getItems().get(getIndex());
                boolean pending = "PENDING".equalsIgnoreCase(deposit.status);
                approve.setDisable(!pending);
                reject.setDisable(!pending);
                approve.setOnAction(e -> reviewDeposit(deposit.requestId, true));
                reject.setOnAction(e -> reviewDeposit(deposit.requestId, false));
                setGraphic(box);
            }
        });
        table.getColumns().add(actions);
        setContent(table);
    }

    private void handleProductsResponse(ResponsePayload response) {
        if (!isSuccess(response)) return;
        Type type = new TypeToken<List<AdminProductDTO>>() {}.getType();
        List<AdminProductDTO> products = gson.fromJson(gson.toJson(response.getData()), type);

        TableView<AdminProductDTO> table = new TableView<>(FXCollections.observableArrayList(products));
        addColumn(table, "Auction", p -> String.valueOf(p.auctionId), 80);
        addColumn(table, "Product", p -> p.itemName, 220);
        addColumn(table, "Seller", p -> p.sellerUsername, 140);
        addColumn(table, "Category", p -> p.category, 120);
        addColumn(table, "Price", p -> "$" + String.format("%.2f", p.startingPrice), 110);
        addColumn(table, "Approval", p -> p.approvalStatus, 120);
        addColumn(table, "Auction", p -> p.auctionStatus, 110);

        TableColumn<AdminProductDTO, Void> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(240);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button view = new Button("View");
            private final Button approve = new Button("Approve");
            private final Button reject = new Button("Reject");
            private final HBox box = new HBox(8, view, approve, reject);

            {
                view.getStyleClass().addAll("table-action-button");
                approve.getStyleClass().addAll("table-action-button", "success-button");
                reject.getStyleClass().addAll("table-action-button", "danger-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AdminProductDTO product = getTableView().getItems().get(getIndex());
                approve.setDisable("APPROVED".equalsIgnoreCase(product.approvalStatus));
                reject.setDisable("REJECTED".equalsIgnoreCase(product.approvalStatus));
                view.setOnAction(e -> showProductDetail(product));
                approve.setOnAction(e -> reviewProduct(product.auctionId, true));
                reject.setOnAction(e -> reviewProduct(product.auctionId, false));
                setGraphic(box);
            }
        });
        table.getColumns().add(actions);
        setContent(table);
    }

    private void handleAuctionsResponse(ResponsePayload response) {
        if (!isSuccess(response)) return;
        Type type = new TypeToken<List<AdminAuctionDTO>>() {}.getType();
        List<AdminAuctionDTO> auctions = gson.fromJson(gson.toJson(response.getData()), type);

        TableView<AdminAuctionDTO> table = new TableView<>(FXCollections.observableArrayList(auctions));
        addColumn(table, "ID", a -> String.valueOf(a.auctionId), 70);
        addColumn(table, "Product", a -> a.itemName, 220);
        addColumn(table, "Seller", a -> a.sellerUsername, 140);
        addColumn(table, "Price", a -> "$" + String.format("%.2f", a.currentPrice), 110);
        addColumn(table, "Status", a -> a.status, 110);
        addColumn(table, "Approval", a -> a.approvalStatus, 120);
        addColumn(table, "Bids", a -> String.valueOf(a.bidCount), 70);
        addColumn(table, "Ends", a -> shorten(a.endTime), 170);

        TableColumn<AdminAuctionDTO, Void> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(240);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button view = new Button("View");
            private final Button finish = new Button("Finish");
            private final Button cancel = new Button("Cancel");
            private final HBox box = new HBox(8, view, finish, cancel);

            {
                view.getStyleClass().addAll("table-action-button");
                finish.getStyleClass().addAll("table-action-button", "warning-button");
                cancel.getStyleClass().addAll("table-action-button", "danger-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AdminAuctionDTO auction = getTableView().getItems().get(getIndex());
                view.setOnAction(e -> showAuctionDetail(auction));
                finish.setOnAction(e -> updateAuctionStatus(auction.auctionId, "FINISHED"));
                cancel.setOnAction(e -> updateAuctionStatus(auction.auctionId, "CANCELED"));
                setGraphic(box);
            }
        });
        table.getColumns().add(actions);
        setContent(table);
    }

    private void handleAuctionCatalogChanged(ResponsePayload response) {
        switch (currentSection) {
            case "DASHBOARD" -> send("GET_ADMIN_DASHBOARD", "{}");
            case "PRODUCTS" -> send("GET_ADMIN_PRODUCTS", "{}");
            case "AUCTIONS" -> send("GET_ADMIN_AUCTIONS", "{}");
            case "WINNERS" -> send("GET_ADMIN_WINNERS", "{}");
            default -> {
            }
        }
    }

    private void handleBidCatalogChanged(ResponsePayload response) {
        switch (currentSection) {
            case "DASHBOARD" -> send("GET_ADMIN_DASHBOARD", "{}");
            case "AUCTIONS" -> send("GET_ADMIN_AUCTIONS", "{}");
            case "WINNERS" -> send("GET_ADMIN_WINNERS", "{}");
            default -> {
            }
        }
    }

    private <T> void addColumn(TableView<T> table, String title, Function<T, String> mapper, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(mapper.apply(cell.getValue()))));
        table.getColumns().add(column);
    }

    private Node metric(String label, Object value) {
        VBox box = new VBox(6);
        box.getStyleClass().add("metric-card");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("metric-label");
        Label valueNode = new Label(String.valueOf(value));
        valueNode.getStyleClass().add("metric-value");
        box.getChildren().addAll(labelNode, valueNode);
        return box;
    }

    private void setUserActive(Long userId, boolean active) {
        send("ADMIN_SET_USER_ACTIVE", "{\"userId\":" + userId + ",\"active\":" + active + "}");
    }

    private void reviewDeposit(Long requestId, boolean approve) {
        Long adminId = Session.getCurrentUser() != null ? Session.getCurrentUser().getId() : null;
        String data = "{\"requestId\":" + requestId + ",\"adminId\":" + adminId + "}";
        send(approve ? "ADMIN_APPROVE_DEPOSIT" : "ADMIN_REJECT_DEPOSIT", data);
    }

    private void reviewProduct(Long auctionId, boolean approve) {
        send(approve ? "ADMIN_APPROVE_PRODUCT" : "ADMIN_REJECT_PRODUCT", "{\"auctionId\":" + auctionId + "}");
    }

    private void updateAuctionStatus(Long auctionId, String status) {
        send("ADMIN_UPDATE_AUCTION_STATUS", "{\"auctionId\":" + auctionId + ",\"status\":\"" + status + "\"}");
    }

    private void send(String action, String data) {
        SocketClient.getInstance().sendRequest(new RequestPayload(action, data));
    }

    private boolean isSuccess(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            return true;
        }
        showLoading(response.getMessage());
        return false;
    }

    private void showLoading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-message");
        setContent(label);
    }

    private void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    private String shorten(String value) {
        if (value == null) return "";
        return value.length() > 19 ? value.substring(0, 19) : value;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // Show product detail dialog
    private void showProductDetail(AdminProductDTO product) {
        StringBuilder sb = new StringBuilder();
        sb.append("Auction ID: ").append(product.auctionId).append("\n");
        sb.append("Product: ").append(product.itemName).append("\n");
        sb.append("Seller: ").append(product.sellerUsername).append("\n");
        sb.append("Category: ").append(product.category).append("\n");
        sb.append("Starting Price: $").append(String.format("%.2f", product.startingPrice)).append("\n");
        sb.append("Approval Status: ").append(product.approvalStatus).append("\n");
        sb.append("Auction Status: ").append(product.auctionStatus);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Product Detail");
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    // Show auction detail dialog
    private void showAuctionDetail(AdminAuctionDTO auction) {
        StringBuilder sb = new StringBuilder();
        sb.append("Auction ID: ").append(auction.auctionId).append("\n");
        sb.append("Product: ").append(auction.itemName).append("\n");
        sb.append("Seller: ").append(auction.sellerUsername).append("\n");
        sb.append("Current Price: $").append(String.format("%.2f", auction.currentPrice)).append("\n");
        sb.append("Status: ").append(auction.status).append("\n");
        sb.append("Approval Status: ").append(auction.approvalStatus).append("\n");
        sb.append("Bid Count: ").append(auction.bidCount).append("\n");
        sb.append("Ends: ").append(auction.endTime);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Auction Detail");
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }
}
