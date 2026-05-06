package com.example.bai_tap_lon.controller;

import com.example.bai_tap_lon.model.AuctionItem;
import com.example.bai_tap_lon.model.AuctionStatus;
import com.example.bai_tap_lon.model.Bid;
import com.example.bai_tap_lon.model.ProductReview;
import com.example.bai_tap_lon.model.User;
import com.example.bai_tap_lon.model.UserRole;
import com.example.bai_tap_lon.service.AuctionException;
import com.example.bai_tap_lon.service.AuctionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainController {
    private final AuctionService service = new AuctionService();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final NumberFormat moneyFormat = NumberFormat.getNumberInstance(Locale.US);

    private User currentUser;

    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private TextField signupFullNameField;
    @FXML
    private TextField signupUsernameField;
    @FXML
    private PasswordField signupPasswordField;
    @FXML
    private ComboBox<UserRole> signupRoleBox;
    @FXML
    private Label authMessageLabel;
    @FXML
    private Label currentUserLabel;
    @FXML
    private Label systemMessageLabel;
    @FXML
    private VBox authPane;
    @FXML
    private VBox authChoicePane;
    @FXML
    private VBox loginPane;
    @FXML
    private VBox signupPane;
    @FXML
    private BorderPane appPane;
    @FXML
    private Button logoutButton;
    @FXML
    private TabPane mainTabs;
    @FXML
    private Tab liveAuctionTab;
    @FXML
    private Tab adminTab;
    @FXML
    private TableView<AuctionItem> auctionTable;
    @FXML
    private TableColumn<AuctionItem, String> nameColumn;
    @FXML
    private TableColumn<AuctionItem, String> priceColumn;
    @FXML
    private TableColumn<AuctionItem, String> statusColumn;
    @FXML
    private TableColumn<AuctionItem, String> endTimeColumn;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailDescriptionLabel;
    @FXML
    private Label detailSellerLabel;
    @FXML
    private Label detailStartLabel;
    @FXML
    private Label detailEndLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailCurrentPriceLabel;
    @FXML
    private Label detailLeaderLabel;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private ComboBox<AuctionItem> bidItemComboBox;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button bidButton;
    @FXML
    private ListView<String> bidHistoryList;
    @FXML
    private ComboBox<Integer> ratingComboBox;
    @FXML
    private TextArea reviewCommentArea;
    @FXML
    private Button submitReviewButton;
    @FXML
    private ListView<String> reviewList;
    @FXML
    private VBox sellerPane;
    @FXML
    private TextField itemNameField;
    @FXML
    private TextArea itemDescriptionArea;
    @FXML
    private TextField itemStartingPriceField;
    @FXML
    private TextField itemStartTimeField;
    @FXML
    private TextField itemEndTimeField;
    @FXML
    private Button addItemButton;
    @FXML
    private Button updateItemButton;
    @FXML
    private Button deleteItemButton;
    @FXML
    private VBox adminPane;
    @FXML
    private Button markPaidButton;
    @FXML
    private Button cancelButton;

    @FXML
    public void initialize() {
        signupRoleBox.setItems(FXCollections.observableArrayList(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN));
        signupRoleBox.setValue(UserRole.BIDDER);
        ratingComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingComboBox.setValue(5);

        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getCurrentHighestPrice())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        endTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue().getEndTime())));

        auctionTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            showItemDetails(newValue);
            fillItemForm(newValue);
            if (newValue != null) {
                bidItemComboBox.getSelectionModel().select(newValue);
            }
        });

        bidItemComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                auctionTable.getSelectionModel().select(newValue);
                showItemDetails(newValue);
            }
        });

        itemStartTimeField.setText(formatDateTime(LocalDateTime.now()));
        itemEndTimeField.setText(formatDateTime(LocalDateTime.now().plusHours(2)));
        refreshView();
        setCurrentUser(null);
        showAuthChoice();
    }

    @FXML
    public void showLoginForm() {
        setAuthFormVisible(loginPane);
        authMessageLabel.setText("");
    }

    @FXML
    public void showSignupForm() {
        setAuthFormVisible(signupPane);
        authMessageLabel.setText("");
    }

    @FXML
    public void showAuthChoice() {
        setAuthFormVisible(authChoicePane);
        authMessageLabel.setText("");
    }

    @FXML
    public void handleLogin() {
        try {
            currentUser = service.login(loginUsernameField.getText(), loginPasswordField.getText());
            authMessageLabel.setText("");
            loginPasswordField.clear();
            setCurrentUser(currentUser);
            showMessage("Dang nhap thanh cong.");
        } catch (AuctionException ex) {
            authMessageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void handleSignup() {
        try {
            currentUser = service.register(
                    signupUsernameField.getText(),
                    signupPasswordField.getText(),
                    signupFullNameField.getText(),
                    signupRoleBox.getValue()
            );
            clearSignupFields();
            currentUser = null;
            setCurrentUser(null);
            showAuthChoice();
            authMessageLabel.setText("Dang ky thanh cong. Hay dang nhap bang tai khoan vua tao.");
        } catch (AuctionException ex) {
            authMessageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        setCurrentUser(null);
        loginUsernameField.clear();
        loginPasswordField.clear();
        showMessage("Da dang xuat.");
    }

    @FXML
    public void handleRefresh() {
        refreshView();
        showMessage("Da cap nhat danh sach phien dau gia.");
    }

    @FXML
    public void handlePlaceBid() {
        try {
            service.placeBid(currentUser, selectedBidItem(), parseMoney(bidAmountField.getText()));
            bidAmountField.clear();
            refreshView();
            showMessage("Dat gia thanh cong.");
        } catch (AuctionException | NumberFormatException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleSubmitReview() {
        try {
            service.reviewItem(currentUser, selectedItem(), ratingComboBox.getValue(), reviewCommentArea.getText());
            reviewCommentArea.clear();
            refreshView();
            showMessage("Da gui danh gia san pham.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleAddItem() {
        try {
            AuctionItem item = service.addItem(
                    currentUser,
                    itemNameField.getText(),
                    itemDescriptionArea.getText(),
                    parseMoney(itemStartingPriceField.getText()),
                    parseDateTime(itemStartTimeField.getText()),
                    parseDateTime(itemEndTimeField.getText())
            );
            refreshView();
            auctionTable.getSelectionModel().select(item);
            showMessage("Da them san pham dau gia.");
        } catch (AuctionException | NumberFormatException | DateTimeParseException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleUpdateItem() {
        try {
            AuctionItem item = selectedItem();
            service.updateItem(
                    currentUser,
                    item,
                    itemNameField.getText(),
                    itemDescriptionArea.getText(),
                    parseMoney(itemStartingPriceField.getText()),
                    parseDateTime(itemStartTimeField.getText()),
                    parseDateTime(itemEndTimeField.getText())
            );
            refreshView();
            auctionTable.getSelectionModel().select(item);
            showMessage("Da cap nhat san pham.");
        } catch (AuctionException | NumberFormatException | DateTimeParseException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleDeleteItem() {
        try {
            service.deleteItem(currentUser, selectedItem());
            refreshView();
            showMessage("Da xoa san pham.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleMarkPaid() {
        try {
            service.markPaid(currentUser, selectedItem());
            refreshView();
            showMessage("Da chuyen phien sang PAID.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        try {
            service.cancel(currentUser, selectedItem());
            refreshView();
            showMessage("Da huy phien dau gia.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void setCurrentUser(User user) {
        currentUser = user;
        boolean loggedIn = user != null;
        authPane.setVisible(!loggedIn);
        authPane.setManaged(!loggedIn);
        appPane.setVisible(loggedIn);
        appPane.setManaged(loggedIn);
        logoutButton.setDisable(!loggedIn);
        currentUserLabel.setText(loggedIn ? user.toString() : "Chua dang nhap");

        boolean isBidder = loggedIn && user.getRole() == UserRole.BIDDER;
        boolean isSeller = loggedIn && user.getRole() == UserRole.SELLER;
        boolean isAdmin = loggedIn && user.getRole() == UserRole.ADMIN;

        bidButton.setDisable(!isBidder);
        submitReviewButton.setDisable(!isBidder);
        sellerPane.setDisable(!isSeller);
        adminPane.setDisable(!isAdmin);
        liveAuctionTab.setDisable(!isBidder);
        adminTab.setDisable(!isAdmin);
        if (loggedIn) {
            if (isBidder) {
                mainTabs.getSelectionModel().select(liveAuctionTab);
            } else if (isSeller) {
                mainTabs.getSelectionModel().select(0);
            } else if (isAdmin) {
                mainTabs.getSelectionModel().select(adminTab);
            }
        }
        refreshView();
    }

    private void setAuthFormVisible(VBox visiblePane) {
        authChoicePane.setVisible(visiblePane == authChoicePane);
        authChoicePane.setManaged(visiblePane == authChoicePane);
        loginPane.setVisible(visiblePane == loginPane);
        loginPane.setManaged(visiblePane == loginPane);
        signupPane.setVisible(visiblePane == signupPane);
        signupPane.setManaged(visiblePane == signupPane);
    }

    private void refreshView() {
        AuctionItem selected = auctionTable.getSelectionModel().getSelectedItem();
        AuctionItem selectedBidItem = bidItemComboBox.getSelectionModel().getSelectedItem();
        var items = FXCollections.observableArrayList(service.getItems());
        auctionTable.setItems(items);
        bidItemComboBox.setItems(items);
        if (selected != null) {
            auctionTable.getItems().stream()
                    .filter(item -> item.getId() == selected.getId())
                    .findFirst()
                    .ifPresent(item -> auctionTable.getSelectionModel().select(item));
        }
        if (selectedBidItem != null) {
            bidItemComboBox.getItems().stream()
                    .filter(item -> item.getId() == selectedBidItem.getId())
                    .findFirst()
                    .ifPresent(item -> bidItemComboBox.getSelectionModel().select(item));
        }
        showItemDetails(auctionTable.getSelectionModel().getSelectedItem());
    }

    private void showItemDetails(AuctionItem item) {
        if (item == null) {
            detailNameLabel.setText("Chua chon phien");
            detailDescriptionLabel.setText("");
            detailSellerLabel.setText("-");
            detailStartLabel.setText("-");
            detailEndLabel.setText("-");
            detailStatusLabel.setText("-");
            detailCurrentPriceLabel.setText("-");
            detailLeaderLabel.setText("-");
            averageRatingLabel.setText("-");
            bidHistoryList.setItems(FXCollections.observableArrayList());
            reviewList.setItems(FXCollections.observableArrayList());
            return;
        }

        detailNameLabel.setText(item.getName());
        detailDescriptionLabel.setText(item.getDescription());
        detailSellerLabel.setText(item.getSeller().getFullName());
        detailStartLabel.setText(formatDateTime(item.getStartTime()));
        detailEndLabel.setText(formatDateTime(item.getEndTime()));
        detailStatusLabel.setText(item.getStatus().name());
        detailCurrentPriceLabel.setText(formatMoney(item.getCurrentHighestPrice()));
        detailLeaderLabel.setText(service.getWinner(item).map(User::getFullName).orElse("Chua co"));
        averageRatingLabel.setText(formatAverageRating(item));
        bidHistoryList.setItems(FXCollections.observableArrayList(
                item.getBids().stream()
                        .map(this::formatBid)
                        .collect(Collectors.toList())
        ));
        reviewList.setItems(FXCollections.observableArrayList(
                item.getReviews().stream()
                        .map(this::formatReview)
                        .collect(Collectors.toList())
        ));
    }

    private void fillItemForm(AuctionItem item) {
        if (item == null) {
            return;
        }
        itemNameField.setText(item.getName());
        itemDescriptionArea.setText(item.getDescription());
        itemStartingPriceField.setText(item.getStartingPrice().toPlainString());
        itemStartTimeField.setText(formatDateTime(item.getStartTime()));
        itemEndTimeField.setText(formatDateTime(item.getEndTime()));
    }

    private AuctionItem selectedItem() throws AuctionException {
        AuctionItem item = auctionTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new AuctionException("Vui long chon mot phien dau gia.");
        }
        return item;
    }

    private AuctionItem selectedBidItem() throws AuctionException {
        AuctionItem item = bidItemComboBox.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new AuctionException("Vui long chon san pham muon dau gia.");
        }
        return item;
    }

    private BigDecimal parseMoney(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new NumberFormatException("Gia tien khong duoc de trong.");
        }
        String normalized = rawValue.trim().replace(",", "");
        return new BigDecimal(normalized);
    }

    private LocalDateTime parseDateTime(String rawValue) {
        return LocalDateTime.parse(rawValue.trim(), dateTimeFormatter);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(dateTimeFormatter);
    }

    private String formatMoney(BigDecimal amount) {
        return moneyFormat.format(amount) + " VND";
    }

    private String formatBid(Bid bid) {
        return formatDateTime(bid.getCreatedAt()) + " - " + bid.getBidder().getFullName() + ": " + formatMoney(bid.getAmount());
    }

    private String formatAverageRating(AuctionItem item) {
        if (item.getReviews().isEmpty()) {
            return "Chua co danh gia";
        }
        return String.format(Locale.US, "%.1f/5 (%d danh gia)", item.getAverageRating(), item.getReviews().size());
    }

    private String formatReview(ProductReview review) {
        return formatDateTime(review.getCreatedAt())
                + " - "
                + review.getReviewer().getFullName()
                + " - "
                + review.getRating()
                + "/5: "
                + review.getComment();
    }

    private void clearSignupFields() {
        signupFullNameField.clear();
        signupUsernameField.clear();
        signupPasswordField.clear();
        signupRoleBox.setValue(UserRole.BIDDER);
    }

    private void showMessage(String message) {
        systemMessageLabel.setText(message);
    }
}
