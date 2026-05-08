package com.example.bai_tap_lon.controller;

import com.example.bai_tap_lon.service.LoginService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SignupController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cbRole;

    private final LoginService loginService = new LoginService();

    @FXML
    private void handleSignup() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String confirm = txtConfirmPassword.getText().trim();
        String role = cbRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        boolean success = loginService.register(username, password,role);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công!\nBạn có thể đăng nhập ngay.");
            goToLogin();
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Username đã tồn tại!" + username + password + role);
        }
    }

    @FXML
    private void goToLogin() {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/bai_tap_lon/login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 400, 480);
            stage.setScene(scene);
            stage.setTitle("Auction App - Đăng nhập");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể chuyển sang màn hình đăng nhập!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}