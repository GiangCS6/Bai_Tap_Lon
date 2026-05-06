package com.example.bai_tap_lon.controller;

import com.example.bai_tap_lon.server.LoginService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.util.List;

public class AdminController {

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colPassword;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TextArea txtLog;

    private final LoginService loginService = new LoginService();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Thêm cột Action (Sửa & Xóa)
        TableColumn<User, Void> actionColumn = new TableColumn<>("Hành động");
        actionColumn.setPrefWidth(200);

        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnDelete = new Button("Xóa");

            {
                btnEdit.setStyle("-fx-base: #FF9800;");
                btnDelete.setStyle("-fx-base: #f44336; -fx-text-fill: white;");

                btnEdit.setOnAction(e -> editUser(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8, btnEdit, btnDelete);
                    setGraphic(hbox);
                }
            }
        };

        actionColumn.setCellFactory(cellFactory);
        tableUsers.getColumns().add(actionColumn);

        tableUsers.setItems(userList);
        refreshUserList();
    }

    @FXML
    private void refreshUserList() {
        userList.clear();
        List<User> users = loginService.getAllUsers();
        userList.addAll(users);
        appendLog("Đã tải " + users.size() + " người dùng.");
    }

    @FXML
    private void openAddUserDialog() {
        // Sau này có thể mở Dialog để thêm user
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm User mới");
        dialog.setHeaderText("Nhập thông tin user");
        dialog.setContentText("Username:");

        /*dialog.showAndWait().ifPresent(username -> {
            // Tạm thời thêm user mẫu, sau này sẽ làm form đầy đủ
            boolean success = loginService.register(username, "123456", "Bidder");
            if (success) {
                appendLog("✅ Đã thêm user: " + username);
                refreshUserList();
            } else {
                appendLog("❌ Username đã tồn tại!");
            }
        });*/
    }

    private void editUser(User user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sửa User");
        alert.setHeaderText("Chức năng sửa user");
        alert.setContentText("User: " + user.getUsername() + "\nTính năng này sẽ được hoàn thiện sau.");
        alert.showAndWait();
    }

    private void deleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc muốn xóa user này?");
        confirm.setContentText("Username: " + user.getUsername());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = loginService.deleteUser(user.getUsername());
                if (deleted) {
                    appendLog("🗑️ Đã xóa user: " + user.getUsername());
                    refreshUserList();
                } else {
                    appendLog("❌ Không thể xóa user!");
                }
            }
        });
    }

    private void appendLog(String message) {
        txtLog.appendText(message + "\n");
    }

    // Model cho TableView
    public static class User {
        private final String username;
        private final String password;
        private final String role;

        public User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
    }
}