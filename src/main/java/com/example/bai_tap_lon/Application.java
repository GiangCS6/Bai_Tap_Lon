package com.example.bai_tap_lon;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = Application.class.getResource("main-view.fxml");
        if (fxml == null) {
            throw new IOException("Khong tim thay main-view.fxml.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        Scene scene = new Scene(fxmlLoader.load(), 1120, 720);
        stage.setTitle("Dau Gia Uy Tin");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();
    }
}
