package com.example.bai_tap_lon;


import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Bắt đầu từ màn hình Đăng nhập thay vì main-view
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/com/example/bai_tap_lon/login-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 400, 480);
        stage.setTitle("Auction App - Đăng nhập");
        stage.setScene(scene);
        stage.setResizable(false);   // Không cho thay đổi kích thước màn hình login
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}