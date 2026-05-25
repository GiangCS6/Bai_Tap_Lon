package com.example.bai_tap_lon.controller;

import com.example.bai_tap_lon.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    // ── Điều hướng scene ──────────────────────────────────────

    @FXML
    public void showLogin(ActionEvent event) throws IOException {
        switchScene(event, "login-view.fxml", 820, 480);
    }

    @FXML
    public void showSignup(ActionEvent event) throws IOException {
        switchScene(event, "signup-view.fxml", 820, 580);
    }

    @FXML
    public void showWelcome(ActionEvent event) throws IOException {
        switchScene(event, "welcomedaugia.fxml", 720, 480);
    }

    // ── Xử lý đăng nhập ──────────────────────────────────────

    @FXML
    public void handleLogin(ActionEvent event) throws IOException {
        if (isBlank(usernameField) || isBlank(passwordField)) {
            showMessage("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }
        switchScene(event, "main-view.fxml", 720, 480);
    }

    // ── Chuyển sang màn hình đăng ký ─────────────────────────

    @FXML
    public void handleSignup(ActionEvent event) throws IOException {
        switchScene(event, "signup-view.fxml", 820, 580);
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) throws IOException {
        switchScene(event, "login-view.fxml", 820, 480);
    }

    // ── Helpers ──────────────────────────────────────────────

    private boolean isBlank(TextField field) {
        return field == null
                || field.getText() == null
                || field.getText().trim().isEmpty();
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }

    private void switchScene(ActionEvent event, String fxmlName,
                             double width, double height) throws IOException {
        URL fxml = Application.class.getResource(fxmlName);
        if (fxml == null) {
            throw new IOException("Không tìm thấy " + fxmlName + ".");
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(new FXMLLoader(fxml).load(), width, height);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}