package com.example.bai_tap_lon.controller;

import javafx.application.Platform;
import com.example.bai_tap_lon.model.AuctionItem;
import com.example.bai_tap_lon.model.AuctionStatus;
import com.example.bai_tap_lon.model.Bid;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {
    private final AuctionService service = new AuctionService();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final NumberFormat moneyFormat = NumberFormat.getNumberInstance(Locale.US);

    private User currentUser;
    private String latestCompletionMessage;

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
    private TableColumn<AuctionItem, String> ownerColumn;
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
    private Label detailStartingPriceLabel;
    @FXML
    private ComboBox<AuctionItem> bidItemComboBox;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button bidButton;
    @FXML
    private Button watchItemButton;
    @FXML
    private ListView<AuctionItem> watchedItemList;
    @FXML
    private Label notificationSummaryLabel;
    @FXML
    private ListView<String> notificationList;
    @FXML
    private ListView<String> bidHistoryList;
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
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> userUsernameColumn;
    @FXML
    private TableColumn<User, String> userFullNameColumn;
    @FXML
    private TableColumn<User, String> userRoleColumn;
    @FXML
    private TableColumn<User, String> userStatusColumn;
    @FXML
    private Button deleteUserButton;
    @FXML
    private Button toggleUserLockButton;
    @FXML
    private Button grantAdminButton;
    @FXML
    private Button revokeAdminButton;
    @FXML
    private TableView<AuctionItem> adminProductTable;
    @FXML
    private TableColumn<AuctionItem, String> adminProductNameColumn;
    @FXML
    private TableColumn<AuctionItem, String> adminProductPriceColumn;
    @FXML
    private TableColumn<AuctionItem, String> adminProductStatusColumn;
    @FXML
    private TableColumn<AuctionItem, String> adminProductSellerColumn;
    @FXML
    private TableColumn<AuctionItem, String> adminProductEndTimeColumn;
    @FXML
    private TextField adminItemNameField;
    @FXML
    private TextArea adminItemDescriptionArea;
    @FXML
    private TextField adminItemStartingPriceField;
    @FXML
    private TextField adminItemStartTimeField;
    @FXML
    private TextField adminItemEndTimeField;
    @FXML
    private TextField adminExtendEndTimeField;
    @FXML
    private Button adminUpdateItemButton;
    @FXML
    private Button adminCancelItemButton;
    @FXML
    private Button adminExtendItemButton;
    @FXML
    private Button adminDeleteItemButton;

    @FXML
    public void initialize() {
        signupRoleBox.setItems(FXCollections.observableArrayList(UserRole.BIDDER, UserRole.SELLER));
        signupRoleBox.setValue(UserRole.BIDDER);

        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getCurrentHighestPrice())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue())));
        ownerColumn.setCellValueFactory(data -> new SimpleStringProperty(formatOwner(data.getValue())));
        endTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue().getEndTime())));
        userUsernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        userFullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        userRoleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().getDisplayName()));
        userStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isLocked() ? "Đã khóa" : "Đang hoạt động"));
        adminProductNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        adminProductPriceColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getCurrentHighestPrice())));
        adminProductStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue())));
        adminProductSellerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeller().getFullName()));
        adminProductEndTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue().getEndTime())));

        auctionTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            showItemDetails(newValue);
            fillItemForm(newValue);
            updateWatchButton(newValue);
            if (newValue != null && newValue.getStatus() == AuctionStatus.RUNNING) {
                bidItemComboBox.getSelectionModel().select(newValue);
            } else {
                bidItemComboBox.getSelectionModel().clearSelection();
            }
        });

        bidItemComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                auctionTable.getSelectionModel().select(newValue);
                showItemDetails(newValue);
            }
        });
        watchedItemList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                auctionTable.getSelectionModel().select(newValue);
                showItemDetails(newValue);
            }
        });
        userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateAdminControls());
        adminProductTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            fillAdminItemForm(newValue);
            updateAdminControls();
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
            showMessage(latestCompletionMessage != null ? latestCompletionMessage : "Đăng nhập thành công.");
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
            authMessageLabel.setText("Đăng ký thành công. Hãy đăng nhập bằng tài khoản vừa tạo.");
        } catch (AuctionException ex) {
            authMessageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        setCurrentUser(null);
        loginUsernameField.clear();
        loginPasswordField.clear();
        showMessage("Đã đăng xuất.");
    }

    @FXML
    public void handleRefresh() {
        refreshView();
        showMessage(latestCompletionMessage != null ? latestCompletionMessage : "Đã cập nhật danh sách phiên đấu giá.");
    }

    @FXML
    public void handlePlaceBid() {
        try {
            AuctionItem item = selectedBidItem();
            service.placeBid(currentUser, item, parseMoney(bidAmountField.getText()));
            bidAmountField.clear();
            refreshView();
            auctionTable.refresh();
            showMessage("Đặt giá thành công.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        } catch (NumberFormatException ex) {
            showMessage("Giá tiền không hợp lệ.");
        }
    }

    @FXML
    public void handleToggleWatch() {
        try {
            AuctionItem item = selectedWatchItem();
            boolean watch = !item.isWatchedBy(currentUser);
            service.setItemWatched(currentUser, item, watch);
            refreshView();
            auctionTable.getSelectionModel().select(item);
            showMessage((watch ? "Đã theo dõi " : "Đã bỏ theo dõi ") + item.getName() + ".");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleAddItem() {
        try {
            service.addItem(
                    currentUser,
                    itemNameField.getText(),
                    itemDescriptionArea.getText(),
                    parseMoney(itemStartingPriceField.getText()),
                    parseDateTime(itemStartTimeField.getText()),
                    parseDateTime(itemEndTimeField.getText())
            );
            refreshView();
            resetAddItemForm();
            showMessage("Đã thêm sản phẩm đấu giá.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        } catch (NumberFormatException ex) {
            showMessage("Giá tiền không hợp lệ.");
        } catch (DateTimeParseException ex) {
            showMessage("Thời gian không đúng định dạng yyyy-MM-dd HH:mm.");
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
            showMessage("Đã cập nhật sản phẩm.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        } catch (NumberFormatException ex) {
            showMessage("Giá tiền không hợp lệ.");
        } catch (DateTimeParseException ex) {
            showMessage("Thời gian không đúng định dạng yyyy-MM-dd HH:mm.");
        }
    }

    @FXML
    public void handleDeleteItem() {
        try {
            service.deleteItem(currentUser, selectedItem());
            refreshView();
            showMessage("Đã xóa sản phẩm.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }


    @FXML
    public void handleMarkPaid() {
        try {
            service.markPaid(currentUser, selectedItem());
            refreshView();
            showMessage("Đã chuyển phiên sang trạng thái đã thanh toán.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        try {
            service.cancel(currentUser, selectedItem());
            refreshView();
            showMessage("Đã hủy phiên đấu giá.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleGrantAdmin() {
        try {
            User selectedUser = selectedUser();
            service.grantAdmin(currentUser, selectedUser);
            refreshView();
            showMessage("Đã cấp quyền admin cho " + selectedUser.getUsername() + ".");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleRevokeAdmin() {
        try {
            User selectedUser = selectedUser();
            service.revokeAdmin(currentUser, selectedUser);
            refreshView();
            showMessage("Đã thu hồi quyền admin của " + selectedUser.getUsername() + ".");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleRefreshUsers() {
        refreshUserTable();
        showMessage("Đã cập nhật danh sách người dùng.");
    }

    @FXML
    public void handleDeleteUser() {
        try {
            User selectedUser = selectedUser();
            service.deleteUser(currentUser, selectedUser);
            refreshView();
            showMessage("Đã xóa tài khoản " + selectedUser.getUsername() + ".");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleToggleUserLock() {
        try {
            User selectedUser = selectedUser();
            boolean lock = !selectedUser.isLocked();
            service.setUserLocked(currentUser, selectedUser, lock);
            refreshView();
            showMessage((lock ? "Đã khóa tài khoản " : "Đã mở khóa tài khoản ") + selectedUser.getUsername() + ".");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleAdminCancelItem() {
        try {
            service.cancel(currentUser, selectedAdminItem());
            refreshView();
            showMessage("Đã hủy phiên đấu giá.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleAdminExtendItem() {
        try {
            service.extendAuction(currentUser, selectedAdminItem(), parseDateTime(adminExtendEndTimeField.getText()));
            refreshView();
            showMessage("Đã gia hạn thời gian đấu giá.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        } catch (DateTimeParseException ex) {
            showMessage("Thời gian không đúng định dạng yyyy-MM-dd HH:mm.");
        }
    }

    @FXML
    public void handleAdminDeleteItem() {
        try {
            service.adminDeleteItem(currentUser, selectedAdminItem());
            refreshView();
            clearAdminItemForm();
            showMessage("Đã xóa sản phẩm.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        }
    }

    @FXML
    public void handleAdminUpdateItem() {
        try {
            AuctionItem item = selectedAdminItem();
            service.adminUpdateItem(
                    currentUser,
                    item,
                    adminItemNameField.getText(),
                    adminItemDescriptionArea.getText(),
                    parseMoney(adminItemStartingPriceField.getText()),
                    parseDateTime(adminItemStartTimeField.getText()),
                    parseDateTime(adminItemEndTimeField.getText())
            );
            refreshView();
            adminProductTable.getSelectionModel().select(item);
            showMessage("Đã chỉnh sửa thông tin sản phẩm.");
        } catch (AuctionException ex) {
            showMessage(ex.getMessage());
        } catch (NumberFormatException ex) {
            showMessage("Giá tiền không hợp lệ.");
        } catch (DateTimeParseException ex) {
            showMessage("Thời gian không đúng định dạng yyyy-MM-dd HH:mm.");
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
        currentUserLabel.setText(loggedIn ? user.toString() : "Chưa đăng nhập");

        boolean isBidder = loggedIn && user.getRole() == UserRole.BIDDER;
        boolean isSeller = loggedIn && user.getRole() == UserRole.SELLER;
        boolean isAdmin = loggedIn && user.getRole() == UserRole.ADMIN;

        bidButton.setDisable(!isBidder);
        watchItemButton.setVisible(isBidder);
        watchItemButton.setManaged(isBidder);
        watchedItemList.setDisable(!isBidder);
        sellerPane.setDisable(!isSeller);
        sellerPane.setVisible(isSeller);
        sellerPane.setManaged(isSeller);
        adminPane.setDisable(!isAdmin);
        liveAuctionTab.setDisable(!isBidder);
        adminTab.setDisable(!isAdmin);
        updateAdminControls();
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
        updateWatchButton(auctionTable.getSelectionModel().getSelectedItem());
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
        AuctionItem selectedWatchedItem = watchedItemList.getSelectionModel().getSelectedItem();
        AuctionItem selectedAdminItem = adminProductTable.getSelectionModel().getSelectedItem();
        List<AuctionItem> currentItems = service.getItems();
        var items = FXCollections.observableArrayList(currentItems);
        var liveItems = FXCollections.observableArrayList(
                currentItems.stream()
                        .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                        .collect(Collectors.toList())
        );
        auctionTable.setItems(items);
        adminProductTable.setItems(FXCollections.observableArrayList(currentItems));
        bidItemComboBox.setItems(liveItems);
        refreshWatchedItems(currentItems);
        refreshNotifications(currentItems);
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
        if (selectedWatchedItem != null) {
            watchedItemList.getItems().stream()
                    .filter(item -> item.getId() == selectedWatchedItem.getId())
                    .findFirst()
                    .ifPresent(item -> watchedItemList.getSelectionModel().select(item));
        }
        if (selectedAdminItem != null) {
            adminProductTable.getItems().stream()
                    .filter(item -> item.getId() == selectedAdminItem.getId())
                    .findFirst()
                    .ifPresent(item -> adminProductTable.getSelectionModel().select(item));
        }
        auctionTable.refresh();
        adminProductTable.refresh();
        showItemDetails(auctionTable.getSelectionModel().getSelectedItem());
        refreshUserTable();
    }

    private void refreshWatchedItems(List<AuctionItem> currentItems) {
        if (currentUser == null || currentUser.getRole() != UserRole.BIDDER) {
            watchedItemList.setItems(FXCollections.observableArrayList());
            return;
        }

        watchedItemList.setItems(FXCollections.observableArrayList(
                currentItems.stream()
                        .filter(item -> item.isWatchedBy(currentUser))
                        .collect(Collectors.toList())
        ));
    }

    private void refreshNotifications(List<AuctionItem> currentItems) {
        List<String> notifications = buildRoleNotifications(currentItems);
        latestCompletionMessage = notifications.isEmpty() ? null : notifications.get(0);

        if (notifications.isEmpty()) {
            notificationSummaryLabel.setText("Thông báo");
            notificationList.setItems(FXCollections.observableArrayList(emptyNotificationMessage()));
            return;
        }

        notificationSummaryLabel.setText("Thông báo mới: " + notifications.size());
        notificationList.setItems(FXCollections.observableArrayList(notifications));
    }

    private void showItemDetails(AuctionItem item) {
        if (item == null) {
            detailNameLabel.setText("Chưa chọn phiên");
            detailDescriptionLabel.setText("Mô tả sản phẩm :");
            detailSellerLabel.setText("-");
            detailStartLabel.setText("-");
            detailEndLabel.setText("-");
            detailStatusLabel.setText("-");
            detailCurrentPriceLabel.setText("-");
            detailLeaderLabel.setText("-");
            detailStartingPriceLabel.setText("-");
            bidHistoryList.setItems(FXCollections.observableArrayList());
            updateWatchButton(null);
            return;
        }

        detailNameLabel.setText(item.getName());
        detailDescriptionLabel.setText("Mô tả sản phẩm : " + item.getDescription());
        detailSellerLabel.setText(item.getSeller().getFullName());
        detailStartLabel.setText(formatDateTime(item.getStartTime()));
        detailEndLabel.setText(formatDateTime(item.getEndTime()));
        detailStatusLabel.setText(formatStatus(item));
        detailCurrentPriceLabel.setText(formatMoney(item.getCurrentHighestPrice()));
        detailLeaderLabel.setText(service.getWinner(item).map(User::getFullName).orElse("Chưa có"));
        detailStartingPriceLabel.setText(formatMoney(item.getStartingPrice()));
        bidHistoryList.setItems(FXCollections.observableArrayList(
                item.getBids().stream()
                        .map(this::formatBid)
                        .collect(Collectors.toList())
        ));
        updateWatchButton(item);
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

    private void clearItemForm() {
        itemNameField.clear();
        itemDescriptionArea.clear();
        itemStartingPriceField.clear();
        itemStartTimeField.setText(formatDateTime(LocalDateTime.now()));
        itemEndTimeField.setText(formatDateTime(LocalDateTime.now().plusHours(2)));
    }

    private void fillAdminItemForm(AuctionItem item) {
        if (item == null) {
            clearAdminItemForm();
            return;
        }
        adminItemNameField.setText(item.getName());
        adminItemDescriptionArea.setText(item.getDescription());
        adminItemStartingPriceField.setText(item.getStartingPrice().toPlainString());
        adminItemStartTimeField.setText(formatDateTime(item.getStartTime()));
        adminItemEndTimeField.setText(formatDateTime(item.getEndTime()));
        adminExtendEndTimeField.setText(formatDateTime(item.getEndTime().plusMinutes(30)));
    }

    private void clearAdminItemForm() {
        adminItemNameField.clear();
        adminItemDescriptionArea.clear();
        adminItemStartingPriceField.clear();
        adminItemStartTimeField.clear();
        adminItemEndTimeField.clear();
        adminExtendEndTimeField.clear();
    }

    private void resetAddItemForm() {
        auctionTable.getSelectionModel().clearSelection();
        bidItemComboBox.getSelectionModel().clearSelection();
        clearItemForm();
        showItemDetails(null);
        itemNameField.requestFocus();
    }

    private AuctionItem selectedItem() throws AuctionException {
        AuctionItem item = auctionTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new AuctionException("Vui lòng chọn một phiên đấu giá.");
        }
        return item;
    }

    private AuctionItem selectedAdminItem() throws AuctionException {
        AuctionItem item = adminProductTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new AuctionException("Vui lòng chọn một sản phẩm.");
        }
        return item;
    }

    private AuctionItem selectedBidItem() throws AuctionException {
        AuctionItem item = bidItemComboBox.getSelectionModel().getSelectedItem();
        if (item == null) {
            throw new AuctionException("Vui lòng chọn sản phẩm muốn đấu giá.");
        }
        return item;
    }

    private AuctionItem selectedWatchItem() throws AuctionException {
        AuctionItem item = auctionTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            item = bidItemComboBox.getSelectionModel().getSelectedItem();
        }
        if (item == null) {
            item = watchedItemList.getSelectionModel().getSelectedItem();
        }
        if (item == null) {
            throw new AuctionException("Vui lòng chọn sản phẩm muốn theo dõi.");
        }
        return item;
    }

    private User selectedUser() throws AuctionException {
        User user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            throw new AuctionException("Vui lòng chọn một tài khoản.");
        }
        return user;
    }

    private void refreshUserTable() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        var users = FXCollections.observableArrayList(service.getUsers());
        userTable.setItems(users);
        if (selected != null) {
            userTable.getItems().stream()
                    .filter(user -> user.getId() == selected.getId())
                    .findFirst()
                    .ifPresent(user -> userTable.getSelectionModel().select(user));
        }
        updateAdminControls();
    }

    private void updateAdminControls() {
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        boolean canGrantAdmin = service.isRootAdmin(currentUser);

        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        boolean hasUser = selectedUser != null;
        boolean selectedRootAdmin = hasUser && service.isRootAdmin(selectedUser);
        boolean selectedSelf = hasUser && currentUser != null && selectedUser.getId() == currentUser.getId();

        deleteUserButton.setDisable(!isAdmin || !hasUser || selectedRootAdmin || selectedSelf);
        toggleUserLockButton.setDisable(!isAdmin || !hasUser || selectedRootAdmin || selectedSelf);
        toggleUserLockButton.setText(hasUser && selectedUser.isLocked() ? "Mở khóa tài khoản" : "Khóa tài khoản");
        grantAdminButton.setDisable(!canGrantAdmin || !hasUser || selectedUser.getRole() == UserRole.ADMIN);
        revokeAdminButton.setDisable(!canGrantAdmin || !hasUser || selectedRootAdmin || selectedUser.getRole() != UserRole.ADMIN);

        boolean hasProduct = adminProductTable.getSelectionModel().getSelectedItem() != null;
        adminUpdateItemButton.setDisable(!isAdmin || !hasProduct);
        adminCancelItemButton.setDisable(!isAdmin || !hasProduct);
        adminExtendItemButton.setDisable(!isAdmin || !hasProduct);
        adminDeleteItemButton.setDisable(!isAdmin || !hasProduct);
    }

    private void updateWatchButton(AuctionItem item) {
        boolean canWatch = currentUser != null && currentUser.getRole() == UserRole.BIDDER && item != null;
        watchItemButton.setDisable(!canWatch);
        if (!canWatch) {
            watchItemButton.setText("Theo dõi sản phẩm");
            return;
        }

        watchItemButton.setText(item.isWatchedBy(currentUser) ? "Bỏ theo dõi" : "Theo dõi sản phẩm");
    }

    private BigDecimal parseMoney(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new NumberFormatException("Giá tiền không được để trống.");
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

    private String formatStatus(AuctionItem item) {
        if (isSuccessfullyAuctioned(item)) {
            return "Đã được đấu giá";
        }
        return item.getStatus().getDisplayName();
    }

    private String formatOwner(AuctionItem item) {
        return service.getWinner(item)
                .filter(winner -> isSuccessfullyAuctioned(item))
                .map(User::getFullName)
                .orElse("-");
    }

    private String formatBid(Bid bid) {
        return formatDateTime(bid.getCreatedAt()) + " - " + bid.getBidder().getFullName() + ": " + formatMoney(bid.getAmount());
    }

    private boolean isSuccessfullyAuctioned(AuctionItem item) {
        return (item.getStatus() == AuctionStatus.FINISHED || item.getStatus() == AuctionStatus.PAID)
                && service.getWinner(item).isPresent();
    }

    private List<String> buildRoleNotifications(List<AuctionItem> items) {
        List<String> notifications = new ArrayList<>();
        if (currentUser == null) {
            return notifications;
        }

        for (AuctionItem item : items) {
            if (currentUser.getRole() == UserRole.BIDDER && item.isWatchedBy(currentUser)) {
                notifications.add(formatBidderNotification(item));
            } else if (currentUser.getRole() == UserRole.SELLER
                    && item.getSeller().getId() == currentUser.getId()
                    && isClosedForNotification(item)) {
                notifications.add(formatSellerNotification(item));
            } else if (currentUser.getRole() == UserRole.ADMIN && isClosedForNotification(item)) {
                notifications.add(formatAdminNotification(item));
            }
        }

        return notifications;
    }

    private String formatBidderNotification(AuctionItem item) {
        if (!isClosedForNotification(item)) {
            return "Đang theo dõi: " + item.getName()
                    + " | Trạng thái: " + formatStatus(item)
                    + " | Giá hiện tại: " + formatMoney(item.getCurrentHighestPrice())
                    + " | Kết thúc: " + formatDateTime(item.getEndTime());
        }

        Optional<User> winner = service.getWinner(item);
        String result = winner
                .map(user -> user.getId() == currentUser.getId()
                        ? "Bạn là người thắng"
                        : "Người thắng: " + user.getFullName())
                .orElse("Chưa có người thắng");

        return "Sản phẩm theo dõi đã kết thúc: " + item.getName()
                + " | Trạng thái: " + formatStatus(item)
                + " | Giá cuối: " + formatMoney(item.getCurrentHighestPrice())
                + " | " + result;
    }

    private String formatSellerNotification(AuctionItem item) {
        return "Sản phẩm của bạn đã kết thúc: " + item.getName()
                + " | Mô tả: " + item.getDescription()
                + " | Trạng thái: " + formatStatus(item)
                + " | Giá khởi điểm: " + formatMoney(item.getStartingPrice())
                + " | Giá cuối: " + formatMoney(item.getCurrentHighestPrice())
                + " | Người thắng: " + service.getWinner(item).map(User::getFullName).orElse("Không có")
                + " | Số lượt đấu giá: " + item.getBids().size()
                + " | Kết thúc: " + formatDateTime(item.getEndTime());
    }

    private String formatAdminNotification(AuctionItem item) {
        return "Phiên đã kết thúc: " + item.getName()
                + " | Người bán: " + item.getSeller().getFullName()
                + " | Trạng thái: " + formatStatus(item)
                + " | Giá cuối: " + formatMoney(item.getCurrentHighestPrice());
    }

    private boolean isClosedForNotification(AuctionItem item) {
        return item.getStatus() == AuctionStatus.FINISHED
                || item.getStatus() == AuctionStatus.PAID
                || item.getStatus() == AuctionStatus.CANCELED;
    }

    private String emptyNotificationMessage() {
        if (currentUser == null) {
            return "Chưa có thông báo.";
        }
        return switch (currentUser.getRole()) {
            case BIDDER -> "Chưa có thông báo. Hãy theo dõi sản phẩm để nhận cập nhật.";
            case SELLER -> "Chưa có thông báo về sản phẩm đã kết thúc.";
            case ADMIN -> "Chưa có thông báo quản trị.";
        };
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
