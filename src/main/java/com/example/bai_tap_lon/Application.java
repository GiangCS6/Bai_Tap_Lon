package com.example.bai_tap_lon;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("/com/example/bai_tap_lon/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 480, 360);

        URL fxml = Application.class.getResource("main-view.fxml");
        if (fxml == null) {
            throw new IOException("Không tìm thấy main-view.fxml.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        Scene scene = new Scene(fxmlLoader.load(), 1120, 720);
        stage.setTitle("Đấu Giá Uy Tín");

        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();
    }
}

