module com.example.bai_tap_lon {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.example.bai_tap_lon to javafx.fxml;
    opens com.example.bai_tap_lon.model to com.google.gson;
    opens com.example.bai_tap_lon.service to com.google.gson;
    
    exports com.example.bai_tap_lon;
    exports com.example.bai_tap_lon.controller;
    opens com.example.bai_tap_lon.controller to javafx.fxml;
}