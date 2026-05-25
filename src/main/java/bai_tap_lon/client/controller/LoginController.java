package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.model.user.Seller;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LoginController {

    /*@FXML private TextField loginEmail; //
    @FXML private PasswordField loginPassword;

    @FXML private TextField regUsername;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;

    @FXML private RadioButton bidderRadio;
    @FXML private RadioButton sellerRadio;

    private static final String FXML_LOGIN    = "/viewfxml/Login.fxml";
    private static final String FXML_REGISTER = "/viewfxml/Register.fxml";
    private static final String FXML_BIDDER   = "/viewfxml/active-auctions.fxml";
    private static final String FXML_SELLER   = "/viewfxml/active-auctions.fxml";
    private static final String FXML_ADMIN    = "/viewfxml/AdminHome.fxml";
    private Client client = Client.getInstance();

    @FXML
    public void onRegister(ActionEvent event) {
        String username = regUsername.getText().trim();
        String email = regEmail.getText().trim();
        String pass = regPassword.getText();
        String confirm = regConfirm.getText();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill all fields");
            return;
        }
        if (!email.endsWith("@gmail.com") || email.length()<=10){
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid email");
            return;
        }
        if (!username.matches("[\\p{L}]+(\\s[\\p{L}]+)*")){
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid username");
            return;
        }
        if (pass.length()<6){
            showAlert(Alert.AlertType.ERROR, "Error", "Password has at least 6 characters");
            return;
        }
        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Password and Confirm Password do not match");
            return;
        }

        if (!bidderRadio.isSelected() && !sellerRadio.isSelected()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a role");
            return;
        }

        String role = bidderRadio.isSelected() ? "BIDDER" : "SELLER";

        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("email", email);
        payload.addProperty("password", pass);
        payload.addProperty("role", role);

        Request request = new Request.Builder()
                .action("REGISTER")
                .payload(payload)
                .build();

        ServerMessageRouter.register("REGISTER",
                                    this::onRegisterSuccess,
                                    this::onRegisterFail);
        client.sendRequest(request);
    }

    public void onRegisterSuccess(JsonObject data) {
            showAlert(Alert.AlertType.INFORMATION, "Successful", "Register success!");
            try {
                switchToLogin();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    public void onRegisterFail(String errorCode, String errorMessage) {
            showAlert(Alert.AlertType.ERROR, errorCode, errorMessage);
    }

    @FXML
    public void onLogin(ActionEvent event) {
        String email = loginEmail.getText().trim();
        String pass = loginPassword.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter your email and your password");
            return;
        }


        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", pass);

        Request request = new Request.Builder()
                .action("LOGIN")
                .payload(payload)
                .build();

        ServerMessageRouter.register("LOGIN",
                                    this::onLoginSuccess,
                                    this::onLoginFail);
        client.sendRequest(request);
    }*/
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;

    @FXML private TextField regUsername;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;

    @FXML private RadioButton bidderRadio;
    @FXML private RadioButton sellerRadio;

    private static final String FXML_LOGIN    = "/viewfxml/Login.fxml";
    private static final String FXML_REGISTER = "/viewfxml/Register.fxml";
    private static final String FXML_BIDDER   = "/viewfxml/active-auctions.fxml";
    private static final String FXML_SELLER   = "/viewfxml/active-auctions.fxml";
    private static final String FXML_ADMIN    = "/viewfxml/AdminHome.fxml";

    private final Client client = Client.getInstance();

    @FXML
    public void onRegister(ActionEvent event) {
        String username = regUsername.getText().trim();
        String email = regEmail.getText().trim();
        String pass = regPassword.getText();
        String confirm = regConfirm.getText();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill all fields");
            return;
        }

        // Removed @gmail.com restriction - now accepts any valid email
        if (!email.contains("@") || email.length() < 5) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid email address");
            return;
        }

        if (!username.matches("[\\p{L}0-9_]+(\\s[\\p{L}0-9_]+)*")) {
            showAlert(Alert.AlertType.ERROR, "Error", "Username can only contain letters, numbers, spaces and underscores");
            return;
        }

        if (pass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Error", "Password must have at least 6 characters");
            return;
        }

        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Password and Confirm Password do not match");
            return;
        }

        if (!bidderRadio.isSelected() && !sellerRadio.isSelected()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a role");
            return;
        }

        String role = bidderRadio.isSelected() ? "BIDDER" : "SELLER";

        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("email", email);
        payload.addProperty("password", pass);
        payload.addProperty("role", role);

        Request request = new Request.Builder()
                .action("REGISTER")
                .payload(payload)
                .build();

        ServerMessageRouter.register("REGISTER", this::onRegisterSuccess, this::onRegisterFail);
        client.sendRequest(request);
    }

    public void onRegisterSuccess(JsonObject data) {
        showAlert(Alert.AlertType.INFORMATION, "Success", "Register successful!");
        try {
            switchToLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onRegisterFail(String errorCode, String errorMessage) {
        showAlert(Alert.AlertType.ERROR, errorCode, errorMessage);
    }

    @FXML
    public void onLogin(ActionEvent event) {
        String username = loginUsername.getText().trim();
        String pass = loginPassword.getText();

        if (username.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter username and password");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);   // Changed to username
        payload.addProperty("password", pass);

        Request request = new Request.Builder()
                .action("LOGIN")
                .payload(payload)
                .build();

        ServerMessageRouter.register("LOGIN", this::onLoginSuccess, this::onLoginFail);
        client.sendRequest(request);
    }

    public void onLoginSuccess(JsonObject data) {
    JsonObject user = (data != null && data.has("user") && data.get("user").isJsonObject())
            ? data.getAsJsonObject("user")
            : null;

    User sessionUser = buildSessionUser(user);
    if (sessionUser == null) {
        Platform.runLater(() ->
            showAlert(Alert.AlertType.ERROR, "Error", "Login response is missing user data"));
        return;
    }

    SessionManager.getInstance().setCurrentUser(sessionUser);
    NotificationStore.getInstance().clear();
    NotificationManager.getInstance().start();

    Platform.runLater(() -> {
        String username = sessionUser.getUsername() == null ? "" : sessionUser.getUsername();
        showAlert(Alert.AlertType.INFORMATION, "Successful", "Login success!\nWelcome: " + username);
        try {
            switchToHome(sessionUser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
}

    public void onLoginFail(String errorCode, String errorMessage) {
            showAlert(Alert.AlertType.ERROR, errorCode, errorMessage);
    }

    @FXML
    public void switchToLogin() throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(FXML_LOGIN)));
        Stage stage = getStage();
        if (stage == null) return;
        stage.getScene().setRoot(root);
    }
    @FXML
    public void switchToRegister() throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(FXML_REGISTER)));
        Stage stage = getStage();
        if (stage == null) return;
        stage.getScene().setRoot(root);
    }

    private Stage getStage() {
        if (loginUsername != null && loginUsername.getScene() != null)
            return (Stage) loginUsername.getScene().getWindow();
        if (regUsername != null && regUsername.getScene() != null)
            return (Stage) regUsername.getScene().getWindow();
        return null;
    }

    private User buildSessionUser(JsonObject user) {
        if (user == null) {
            return null;
        }

        String username = getString(user, "username");
        String email = getString(user, "email");
        String role = getString(user, "role");

        User sessionUser;
        if ("SELLER".equalsIgnoreCase(role)) {
            Seller seller = new Seller(username, null, email);
            if (user.has("balance") && !user.get("balance").isJsonNull()) {
                seller.setBalance(user.get("balance").getAsLong());
            }
            sessionUser = seller;
        } else if ("BIDDER".equalsIgnoreCase(role)) {
            Bidder bidder = new Bidder(username, null, email);
            if (user.has("balance") && !user.get("balance").isJsonNull()) {
                bidder.setBalance(user.get("balance").getAsLong());
            }
            sessionUser = bidder;
        } else {
            final String resolvedRole = role;
            sessionUser = new User(username, null, email) {
                @Override
                public void printInfo() {
                }

                @Override
                public String getRole() {
                    return resolvedRole;
                }
            };
        }

        if (user.has("id") && !user.get("id").isJsonNull()) {
            sessionUser.setId(user.get("id").getAsString());
        }
        if (user.has("createdAt") && !user.get("createdAt").isJsonNull()) {
            sessionUser.setCreatedAt(TimeUtil.fromIso(user.get("createdAt").getAsString()));
        }
        if (user.has("isActive") && !user.get("isActive").isJsonNull()) {
            sessionUser.setActive(user.get("isActive").getAsBoolean());
        }

        return sessionUser;
    }

    private String getString(JsonObject data, String key) {
        return (data.has(key) && !data.get(key).isJsonNull()) ? data.get(key).getAsString() : "";
    }

    public void switchToHome(User sessionUser) throws IOException {
        if (sessionUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No logged-in user available");
            return;
        }

        String role = sessionUser.getRole();
        String fxmlFile;

        if ("BIDDER".equals(role)) {
            fxmlFile = FXML_BIDDER;
        } else if ("SELLER".equals(role)) {
            fxmlFile = FXML_SELLER;
        } else if ("ADMIN".equals(role)) {
            fxmlFile = FXML_ADMIN;
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Unknown role: " + role);
            return;
        }

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlFile)));
        Parent root = loader.load();

        Stage stage = getStage();
        if (stage == null) return;
        stage.getScene().setRoot(root);
        stage.setMaximized(true);
        stage.show();
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
