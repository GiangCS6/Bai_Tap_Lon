package com.example.bai_tap_lon.controller;

import com.example.bai_tap_lon.server.LoginService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private Button btnLogin;

    private final LoginService loginService = new LoginService();

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cbRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin và chọn vai trò!");
            return;
        }

        boolean success = loginService.authenticate(username, password, role);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công với vai trò: " + role);
            openMainScreen(role);
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tài khoản, mật khẩu hoặc vai trò!");
        }
    }

    private void openMainScreen(String role) {
        try {
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            String fxmlPath;
            String title;

            switch (role) {
                case "Seller":
                    fxmlPath = "/com/example/bai_tap_lon/seller-view.fxml";
                    title = "Seller - Đăng bán sản phẩm";
                    break;
                case "Admin":
                    fxmlPath = "/com/example/bai_tap_lon/admin-view.fxml";
                    title = "Admin - Quản lý Market";
                    break;
                case "Bidder":
                default:
                    fxmlPath = "/com/example/bai_tap_lon/main-view.fxml";
                    title = "Bidder - Đấu giá";
                    break;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 950, 720);

            stage.setScene(scene);
            stage.setTitle(title);
            stage.setResizable(true);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình theo vai trò!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void goToSignup() {
        try {
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/bai_tap_lon/signup-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 380, 450);
            stage.setScene(scene);
            stage.setTitle("Đăng ký tài khoản");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}