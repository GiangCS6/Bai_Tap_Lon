module com.example.bai_tap_lon {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;
    requires jdk.httpserver;
    requires java.net.http;

    /*opens bai_tap_lon to javafx.fxml;
    opens bai_tap_lon.common.model to com.google.gson;
    opens bai_tap_lon.service to com.google.gson;
    opens bai_tap_lon.client.controller to javafx.fxml;
    opens bai_tap_lon.client to javafx.fxml;
    opens bai_tap_lon.common.model.item to com.google.gson;
    opens bai_tap_lon.common.model.entity to com.google.gson;
    opens bai_tap_lon.common.model.user to com.google.gson;
    opens bai_tap_lon.server.service to com.google.gson;*/

    // ── Client app entry point ──
//    exports bai_tap_lon.client;
    opens bai_tap_lon.client to javafx.graphics, javafx.fxml;

    // ── Client controllers (FXMLLoader cần reflection) ──
    exports bai_tap_lon.client.controller;
    opens bai_tap_lon.client.controller to javafx.fxml;

    // ── Common models & network (Gson cần reflection để serialize) ──
    opens bai_tap_lon.common.network to com.google.gson;
    opens bai_tap_lon.common.model.entity to com.google.gson;
    opens bai_tap_lon.common.model.user to com.google.gson;
    opens bai_tap_lon.common.model.item to com.google.gson;
}
