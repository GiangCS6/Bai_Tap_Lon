module com.example.bai_tap_lon {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.bai_tap_lon to javafx.fxml;
    exports com.example.bai_tap_lon;
    exports com.example.bai_tap_lon.controller;
    opens com.example.bai_tap_lon.controller to javafx.fxml;
}