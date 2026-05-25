package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.network.Request;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class AdminController {


    @FXML private TextField txtBanUsername;
    @FXML private ComboBox<String> cbReason;
    @FXML private ComboBox<String> cbDuration;

    @FXML
    public void initialize() {
        cbReason.getItems().addAll(
                "Auction spam (Bombing listings)",

                "Fraud, using fake accounts",

                "Inappropriate language",

                "Posting prohibited/illegal products"
        );

        cbDuration.getItems().addAll(
                "3 Days",
                "7 Days",
                "30 Days",
                "Permanent ban"
        );
        cbDuration.getSelectionModel().selectFirst();
    }

    @FXML
    public void onBanUserClick(ActionEvent event) {
        String targetUsername = txtBanUsername.getText().trim();
        String reason = cbReason.getValue();
        String duration = cbDuration.getValue();

        if (targetUsername.isEmpty() || reason == null || reason.trim().isEmpty() || duration == null) {
            showAlert(Alert.AlertType.WARNING, "Error", "Please enter full reason for banning!");
            return;
        }

        if (!"Permanent ban".equals(duration) && !confirmTemporaryBanFallback(duration)) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("targetUsername", targetUsername);
        payload.addProperty("reason", reason.trim());
        payload.addProperty("duration", duration);

        Request request = new Request.Builder()
                .action("BAN_USER")
                .payload(payload)
                .build();

        ServerMessageRouter.register("BAN_USER",
                                    this::onBanUserSuccess,
                                    this::onBanUserFail);
        Client.getInstance().sendRequest(request);
    }

    public void onBanUserSuccess(JsonObject data) {
        String targetUsername = (data != null && data.has("targetUsername") && !data.get("targetUsername").isJsonNull())
                ? data.get("targetUsername").getAsString()
                : txtBanUsername.getText().trim();
        showAlert(Alert.AlertType.INFORMATION, "Successful", "User has been banned: " + targetUsername);
        txtBanUsername.clear();
    }

    public void onBanUserFail(String errorCode, String errorMessage) {
        showAlert(Alert.AlertType.ERROR, errorCode, errorMessage);
    }

    private boolean confirmTemporaryBanFallback(String duration) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Temporary ban notice");
        confirm.setHeaderText("Server has not enabled auto-unban yet");
        confirm.setContentText("Selected duration: " + duration + ".\n"
                + "This account will stay banned until manual unban.\n"
                + "Continue?");
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    @FXML
    public void onLogout(ActionEvent event) {
        try {
            SessionManager.getInstance().clear();
            Parent root = FXMLLoader.load(getClass().getResource("/viewfxml/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to return to the login screen.!");
        }
    }

    // Hàm tiện ích để hiện bảng thông báo popup
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
