package Controller.user;

import Service.core.SceneNavigator;
import Session.Session;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dto.DepositRequest;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.UserAuctionResultDTO;
import dto.WalletTransactionDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import network.SocketClient;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ProfileController {

    @FXML private Label lblUsername;
    @FXML private Label lblBalance;
    @FXML private Label lblAvailableBalance;
    @FXML private Label lblHeldBalance;
    @FXML private Label proofLabel;
    @FXML private Label walletHistoryStatusLabel;
    @FXML private Label auctionResultsTitleLabel;
    @FXML private Label auctionResultsStatusLabel;
    @FXML private ImageView qrImageView;
    @FXML private ImageView imgProfileAvatar;
    @FXML private VBox auctionResultsSection;
    @FXML private TableView<UserAuctionResultDTO> auctionResultsTable;
    @FXML private TableColumn<UserAuctionResultDTO, String> resultTimeColumn;
    @FXML private TableColumn<UserAuctionResultDTO, String> resultItemColumn;
    @FXML private TableColumn<UserAuctionResultDTO, String> resultOtherUserColumn;
    @FXML private TableColumn<UserAuctionResultDTO, String> resultPriceColumn;
    @FXML private TableColumn<UserAuctionResultDTO, String> resultStatusColumn;
    @FXML private TableColumn<UserAuctionResultDTO, String> resultNoteColumn;
    @FXML private TableView<WalletTransactionDTO> walletTransactionsTable;
    @FXML private TableColumn<WalletTransactionDTO, String> txTimeColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txTypeColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txAmountColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txBalanceBeforeColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txBalanceAfterColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txAvailableBeforeColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txAvailableAfterColumn;
    @FXML private TableColumn<WalletTransactionDTO, String> txNoteColumn;

    private final Gson gson = new Gson();
    private String proofImagePath = "";
    private double pendingAmount = 0;

    @FXML
    public void initialize() {
        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("SUBMIT_DEPOSIT_RESPONSE");
        socketClient.clearListeners("BALANCE_UPDATE");
        socketClient.clearListeners("GET_BALANCE_RESPONSE");
        socketClient.clearListeners("GET_WALLET_TRANSACTIONS_RESPONSE");
        socketClient.clearListeners("WALLET_HISTORY_CHANGED");
        socketClient.clearListeners("GET_MY_WIN_LIST_RESPONSE");
        socketClient.clearListeners("GET_MY_SOLD_LIST_RESPONSE");
        socketClient.clearListeners("USER_AUCTION_RESULTS_CHANGED");
        socketClient.on("SUBMIT_DEPOSIT_RESPONSE", this::handleDepositSubmitResponse);
        socketClient.on("BALANCE_UPDATE", this::handleBalanceUpdate);
        socketClient.on("GET_BALANCE_RESPONSE", this::handleBalanceUpdate);
        socketClient.on("GET_WALLET_TRANSACTIONS_RESPONSE", this::handleWalletTransactions);
        socketClient.on("WALLET_HISTORY_CHANGED", response -> fetchWalletTransactions());
        socketClient.on("GET_MY_WIN_LIST_RESPONSE", this::handleAuctionResultsResponse);
        socketClient.on("GET_MY_SOLD_LIST_RESPONSE", this::handleAuctionResultsResponse);
        socketClient.on("USER_AUCTION_RESULTS_CHANGED", response -> fetchAuctionResults());

        setupWalletTransactionTable();
        setupAuctionResultsTable();

        if (Session.getCurrentUser() != null) {
            lblUsername.setText(Session.getCurrentUser().getUsername());
            updateBalanceUI();
            setupProfileAvatar();
        }
        requestBalanceRefresh();
        fetchWalletTransactions();
        fetchAuctionResults();

        try (InputStream qrStream = getClass().getResourceAsStream("/images/myqr.png")) {
            if (qrStream != null) {
                qrImageView.setImage(new Image(qrStream));
            }
        } catch (Exception e) {
            System.out.println("Cannot load QR image: " + e.getMessage());
        }
    }

    private void setupWalletTransactionTable() {
        if (walletTransactionsTable == null) {
            return;
        }

        txTimeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatTime(cell.getValue().createdAt)));

        txTypeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(displayType(cell.getValue().type)));

        txAmountColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatSignedMoney(cell.getValue().amount)));

        txBalanceBeforeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatMoney(cell.getValue().balanceBefore)));

        txBalanceAfterColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatMoney(cell.getValue().balanceAfter)));

        txAvailableBeforeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatMoney(cell.getValue().availableBefore)));

        txAvailableAfterColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatMoney(cell.getValue().availableAfter)));

        txNoteColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(nullToEmpty(cell.getValue().note)));

        txAmountColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);

                WalletTransactionDTO tx = getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex())
                        : null;

                if (tx != null && tx.amount > 0) {
                    setStyle("-fx-text-fill: #0f7a2a; -fx-font-weight: bold;");
                } else if (tx != null && tx.amount < 0) {
                    setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #000000;");
                }
            }
        });

        txNoteColumn.setCellFactory(column -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setStyle("-fx-text-fill: #000000;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                label.setText(item);
                label.setMaxWidth(Math.max(0, txNoteColumn.getWidth() - 10));
                setGraphic(label);
            }
        });

        walletTransactionsTable.setPlaceholder(new Label("No transaction history yet."));
    }

    private void setupAuctionResultsTable() {
        if (auctionResultsTable == null) {
            return;
        }

        resultTimeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatTime(cell.getValue().createdAt)));

        resultItemColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(nullToEmpty(cell.getValue().itemName)));

        resultOtherUserColumn.setCellValueFactory(cell -> {
            UserAuctionResultDTO dto = cell.getValue();

            if (isSeller()) {
                return new SimpleStringProperty("Winner: " + nullToEmpty(dto.winnerUsername));
            }

            return new SimpleStringProperty("Seller: " + nullToEmpty(dto.sellerUsername));
        });

        resultPriceColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatMoney(cell.getValue().finalPrice)));

        resultStatusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(nullToEmpty(cell.getValue().status)));

        resultNoteColumn.setCellValueFactory(cell -> {
            UserAuctionResultDTO dto = cell.getValue();

            if (isSeller()) {
                return new SimpleStringProperty(
                        "Da ban cho " + nullToEmpty(dto.winnerUsername)
                                + " voi gia " + formatMoney(dto.finalPrice)
                );
            }

            return new SimpleStringProperty(
                    "Ban da thang san pham nay voi gia " + formatMoney(dto.finalPrice)
            );
        });

        auctionResultsTable.setPlaceholder(new Label("No auction results yet."));
    }

    private void updateBalanceUI() {
        if (Session.getCurrentUser() == null) {
            return;
        }
        lblBalance.setText("$" + String.format("%.2f", Session.getCurrentUser().getBalance()));
        if (lblAvailableBalance != null) {
            lblAvailableBalance.setText("$" + String.format("%.2f", Session.getCurrentUser().getAvailableBalance()));
        }
        if (lblHeldBalance != null) {
            double held = Math.max(0, Session.getCurrentUser().getBalance() - Session.getCurrentUser().getAvailableBalance());
            lblHeldBalance.setText("$" + String.format("%.2f", held));
        }
    }

    private void requestBalanceRefresh() {
        if (Session.getCurrentUser() != null) {
            SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));
        }
    }

    private void fetchWalletTransactions() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        setWalletHistoryStatus("Loading transaction history...");

        SocketClient.getInstance().sendRequest(new RequestPayload(
                "GET_WALLET_TRANSACTIONS",
                "{\"userId\":" + Session.getCurrentUser().getId() + "}"
        ));
    }

    private void fetchAuctionResults() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        Long userId = Session.getCurrentUser().getId();

        if (auctionResultsStatusLabel != null) {
            auctionResultsStatusLabel.setText("Loading auction results...");
        }

        if (isSeller()) {
            if (auctionResultsTitleLabel != null) {
                auctionResultsTitleLabel.setText("SOLD LIST");
            }

            SocketClient.getInstance().sendRequest(new RequestPayload(
                    "GET_MY_SOLD_LIST",
                    "{\"userId\":" + userId + "}"
            ));

        } else {
            if (auctionResultsTitleLabel != null) {
                auctionResultsTitleLabel.setText("WIN LIST");
            }

            SocketClient.getInstance().sendRequest(new RequestPayload(
                    "GET_MY_WIN_LIST",
                    "{\"userId\":" + userId + "}"
            ));
        }
    }

    private void setWalletHistoryStatus(String message) {
        if (walletHistoryStatusLabel == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            walletHistoryStatusLabel.setText(message);
            return;
        }

        Platform.runLater(() -> walletHistoryStatusLabel.setText(message));
    }

    private void setupProfileAvatar() {
        if (Session.getCurrentUser() == null || imgProfileAvatar == null) return;

        try {
            String userAvatarPath = Session.getCurrentUser().getAvatarPath();
            if (userAvatarPath != null && !userAvatarPath.isEmpty()) {
                imgProfileAvatar.setImage(new Image(userAvatarPath));
            } else {
                InputStream is = getClass().getResourceAsStream("/images/defaultavatar.png");
                if (is != null) imgProfileAvatar.setImage(new Image(is));
            }
        } catch (Exception e) {
            System.out.println("Cannot load avatar: " + e.getMessage());
        }

        double width = imgProfileAvatar.getFitWidth() > 0 ? imgProfileAvatar.getFitWidth() : 100;
        double height = imgProfileAvatar.getFitHeight() > 0 ? imgProfileAvatar.getFitHeight() : 100;
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgProfileAvatar.setClip(clip);
    }

    @FXML
    private void handleAddMoney() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Enter the amount you transferred");
        dialog.setContentText("Amount ($):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double amount = Double.parseDouble(result.get());
                if (amount > 0) {
                    pendingAmount = amount;
                    proofLabel.setText("Pending proof upload for $" + String.format("%.2f", amount));
                }
            } catch (Exception e) {
                proofLabel.setText("Invalid amount.");
            }
        }
    }

    @FXML
    private void chooseProofImage() {
        proofImagePath = "uploaded_proof_bill.png";
        proofLabel.setText("Proof uploaded. Submit for admin approval.");
    }

    @FXML
    private void handleFakePayment() {
        if (pendingAmount <= 0 || proofImagePath.isEmpty()) {
            proofLabel.setText("Enter amount and upload proof first.");
            return;
        }

        DepositRequest deposit = new DepositRequest();
        deposit.userId = Session.getCurrentUser().getId();
        deposit.amount = pendingAmount;
        deposit.proofImagePath = proofImagePath;

        SocketClient.getInstance().sendRequest(new RequestPayload("SUBMIT_DEPOSIT", gson.toJson(deposit)));
    }

    private void handleDepositSubmitResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
                new Alert(Alert.AlertType.INFORMATION, "Deposit request submitted. Please wait for admin approval.").show();
                proofLabel.setText("Deposit request is pending admin approval.");
                pendingAmount = 0;
                proofImagePath = "";
                fetchWalletTransactions();
            } else {
                proofLabel.setText("System error: " + response.getMessage());
            }
        });
    }

    private void handleBalanceUpdate(ResponsePayload response) {
        Platform.runLater(() -> {
            try {
                if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
                    return;
                }

                JsonObject data = JsonParser.parseString(gson.toJson(response.getData())).getAsJsonObject();
                Long userId = data.has("userId") ? data.get("userId").getAsLong() : null;

                if (Session.getCurrentUser() != null
                        && userId != null
                        && Session.getCurrentUser().getId().equals(userId)) {
                    double balance = data.get("balance").getAsDouble();
                    double availableBalance = data.has("availableBalance")
                            ? data.get("availableBalance").getAsDouble()
                            : balance;

                    Session.getCurrentUser().setBalance(balance);
                    Session.getCurrentUser().setAvailableBalance(availableBalance);
                }

                updateBalanceUI();
                fetchWalletTransactions();
            } catch (Exception e) {
                System.err.println("Cannot handle balance update: " + e.getMessage());
                updateBalanceUI();
                fetchWalletTransactions();
            }
        });
    }

    private void handleWalletTransactions(ResponsePayload response) {
        Platform.runLater(() -> {
            if (walletTransactionsTable == null) {
                if (walletHistoryStatusLabel != null) {
                    walletHistoryStatusLabel.setText("Wallet table is not found in FXML.");
                }
                return;
            }

            if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
                walletTransactionsTable.setItems(FXCollections.observableArrayList());
                walletTransactionsTable.refresh();

                if (walletHistoryStatusLabel != null) {
                    walletHistoryStatusLabel.setText("Cannot load transaction history: " + response.getMessage());
                }
                return;
            }

            try {
                Type listType = new TypeToken<List<WalletTransactionDTO>>() {}.getType();

                List<WalletTransactionDTO> transactions =
                        gson.fromJson(gson.toJson(response.getData()), listType);

                List<WalletTransactionDTO> safeTransactions =
                        transactions == null ? List.of() : transactions;

                walletTransactionsTable.setItems(FXCollections.observableArrayList(safeTransactions));
                walletTransactionsTable.refresh();

                if (walletHistoryStatusLabel != null) {
                    if (safeTransactions.isEmpty()) {
                        walletHistoryStatusLabel.setText("No transaction history yet.");
                    } else {
                        walletHistoryStatusLabel.setText(safeTransactions.size() + " transaction(s) loaded.");
                    }
                }

                System.out.println("Wallet transactions rendered in TableView: " + safeTransactions.size());

            } catch (Exception e) {
                walletTransactionsTable.setItems(FXCollections.observableArrayList());
                walletTransactionsTable.refresh();

                if (walletHistoryStatusLabel != null) {
                    walletHistoryStatusLabel.setText("Cannot parse transaction history: " + e.getMessage());
                }

                e.printStackTrace();
            }
        });
    }

    private void handleAuctionResultsResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            if (auctionResultsTable == null) {
                return;
            }

            if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
                auctionResultsTable.setItems(FXCollections.observableArrayList());
                auctionResultsTable.refresh();

                if (auctionResultsStatusLabel != null) {
                    auctionResultsStatusLabel.setText("Cannot load auction results: " + response.getMessage());
                }
                return;
            }

            try {
                Type listType = new TypeToken<List<UserAuctionResultDTO>>() {}.getType();

                List<UserAuctionResultDTO> results =
                        gson.fromJson(gson.toJson(response.getData()), listType);

                List<UserAuctionResultDTO> safeResults =
                        results == null ? List.of() : results;

                auctionResultsTable.setItems(FXCollections.observableArrayList(safeResults));
                auctionResultsTable.refresh();

                if (auctionResultsStatusLabel != null) {
                    if (safeResults.isEmpty()) {
                        auctionResultsStatusLabel.setText(
                                isSeller()
                                        ? "No sold auctions yet."
                                        : "No winning auctions yet."
                        );
                    } else {
                        auctionResultsStatusLabel.setText(
                                safeResults.size()
                                        + (isSeller()
                                        ? " sold auction(s) loaded."
                                        : " winning auction(s) loaded.")
                        );
                    }
                }

            } catch (Exception e) {
                auctionResultsTable.setItems(FXCollections.observableArrayList());
                auctionResultsTable.refresh();

                if (auctionResultsStatusLabel != null) {
                    auctionResultsStatusLabel.setText("Cannot parse auction results: " + e.getMessage());
                }

                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleRefreshWalletHistory(ActionEvent event) {
        fetchWalletTransactions();
    }

    @FXML
    private void handleRefreshAuctionResults() {
        fetchAuctionResults();
    }

    private String displayType(String type) {
        if (type == null) {
            return "";
        }

        return switch (type) {
            case "DEPOSIT_REQUEST" -> "Yeu cau nap tien";
            case "BID_HOLD" -> "Giu tien dat gia";
            case "BID_RELEASE" -> "Hoan tien giu";
            case "AUCTION_PAYMENT" -> "Thanh toan dau gia";
            case "AUCTION_SALE_INCOME" -> "Tien ban san pham";
            case "DEPOSIT" -> "Nap tien da duyet";
            case "REFUND" -> "Hoan tien";
            case "ADJUSTMENT" -> "Dieu chinh";
            default -> type;
        };
    }

    private String formatSignedMoney(double amount) {
        if (amount > 0) {
            return "+$" + String.format("%.2f", amount);
        }

        if (amount < 0) {
            return "-$" + String.format("%.2f", Math.abs(amount));
        }

        return "$0.00";
    }

    private String formatMoney(double amount) {
        return "$" + String.format("%.2f", amount);
    }

    private String formatTime(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        try {
            return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return value.replace("T", " ");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isSeller() {
        return Session.getCurrentUser() != null
                && Session.getCurrentUser().getRole() != null
                && Session.getCurrentUser().getRole().equalsIgnoreCase("SELLER");
    }

    @FXML
    private void goHome(ActionEvent event) {
        if (Session.getCurrentUser() != null
                && "ADMIN".equalsIgnoreCase(Session.getCurrentUser().getRole())) {
            SceneNavigator.loadFromNode(lblUsername, "/ui/user/AdminView.fxml", "Admin Dashboard");
        } else {
            SceneNavigator.loadFromNode(lblUsername, "/ui/product/AuctionMain.fxml", "San dau gia");
        }
    }

    @FXML
    private void handleUploadAvatar(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose avatar");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String newAvatarPath = selectedFile.toURI().toString();
            Session.getCurrentUser().setAvatarPath(newAvatarPath);
            imgProfileAvatar.setImage(new Image(newAvatarPath));
        }
    }
}
