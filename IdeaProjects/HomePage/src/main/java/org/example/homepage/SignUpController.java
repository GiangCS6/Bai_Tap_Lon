package org.example.homepage;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SignUpController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField emailField;

    @FXML
    private Label messageLabel;

    public void handleSignUp() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, usernameField.getText());
            stmt.setString(2, passwordField.getText());
            stmt.setString(3, emailField.getText());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                messageLabel.setText("Đăng ký thành công!");
                // Sau đó có thể chuyển sang màn hình login hoặc homepage
            } else {
                messageLabel.setText("Đăng ký thất bại!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Có lỗi xảy ra khi đăng ký.");
        }
    }
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
