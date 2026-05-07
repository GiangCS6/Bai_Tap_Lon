package org.example.homepage;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    public void handleLogin() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, usernameField.getText());
            stmt.setString(2, passwordField.getText());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Đăng nhập thành công -> chuyển sang homepage
            } else {
                // Báo lỗi đăng nhập
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected Label welcomeText;

    public LoginController() throws SQLException {
    }

    @FXML
    protected void onSignUpButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

}
