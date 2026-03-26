package com.example.bai_tap_lon;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("login-"));
        Scene scene = new Scene(fxmlLoader.load(), 480, 360);
        stage.setScene(scene);
        stage.show();
    }
}

