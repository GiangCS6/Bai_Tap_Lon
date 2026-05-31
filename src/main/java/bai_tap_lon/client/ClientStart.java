package bai_tap_lon.client;

import bai_tap_lon.client.network.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * ClientApp — Entry point của ứng dụng JavaFX phía client.
 *
 * Trách nhiệm:
 *   1. Kết nối socket đến Server (mặc định localhost:8000)
 *   2. Load màn hình đầu tiên: Login.fxml
 *   3. Đăng ký callback khi mất kết nối
 *   4. Đóng kết nối socket sạch sẽ khi user đóng app
 *
 * Chạy bằng:  mvn clean javafx:run
 * (mainClass đã được khai báo trong pom.xml: bai_tap_lon.client.ClientApp)
 */
public class ClientStart extends Application {

    // ──────────────────────────────────────────────────────
    //  CONFIG
    // ──────────────────────────────────────────────────────
    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 8000;

    private static final String FXML_LOGIN  = "/viewfxml/Login.fxml";
    private static final String APP_TITLE   = "VN Bay";
    private static final double WIN_WIDTH   = 1400;
    private static final double WIN_HEIGHT  = 900;
    private static final double MIN_WIDTH   = 1000;
    private static final double MIN_HEIGHT  = 700;

    // Static reference để có thể load screen từ bất kỳ controller nào
    private static Stage primaryStage;

    // ──────────────────────────────────────────────────────
    //  JAVAFX LIFECYCLE
    // ──────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        ClientStart.primaryStage = stage;

        // 1) Kết nối tới server
        Client client = Client.getInstance();
        try {
            client.connect(SERVER_HOST, SERVER_PORT);
        } catch (IOException e) {
            // Hiện popup báo lỗi rồi thoát app — không có server thì không làm gì được
            showFatalAlert(
                    "Cannot connect to server",
                    "Cannot connect to " + SERVER_HOST + ":" + SERVER_PORT
                            + "\nPlease make sure the server is running.\n\nDetails: " + e.getMessage()
            );
            Platform.exit();
            return;
        }

        // 2) Khi mất kết nối giữa chừng → cảnh báo + thoát
        client.setOnDisconnected(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Cannot connect to server");
            a.setHeaderText(null);
            a.setContentText("Cannot connect to server. Application will close.");
            a.showAndWait();
            Platform.exit();
        });

        // 3) Load Login.fxml làm màn hình đầu tiên
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource(FXML_LOGIN),
                            "Cannot find " + FXML_LOGIN)
            );

            stage.setTitle(APP_TITLE);
            stage.setScene(new Scene(root, WIN_WIDTH, WIN_HEIGHT));
            stage.setMinWidth(MIN_WIDTH);
            stage.setMinHeight(MIN_HEIGHT);
            stage.setOnShown(e -> stage.setMaximized(true));
            stage.show();

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            showFatalAlert(
                    "Error loading interface",
                    "Cannot load " + FXML_LOGIN + "\nDetails: " + e.getMessage()
            );
            Platform.exit();
        }

    }

    @Override
    public void stop() {
        // Được gọi khi user đóng cửa sổ → đóng socket sạch sẽ
        try {
            Client.getInstance().disconnect();
        } catch (Exception ignored) {
        }
    }

    // ──────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────

    /**
     * Load và hiển thị một FXML screen với sizing phù hợp
     * @param fxmlPath Đường dẫn tới FXML file (ví dụ: "/viewfxml/Login.fxml")
     */
    public static void loadScreen(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(ClientStart.class.getResource(fxmlPath),
                            "Cannot find " + fxmlPath)
            );

            if (primaryStage != null) {
                // Lấy kích thước hiện tại của cửa sổ
                double currentWidth = primaryStage.getWidth();
                double currentHeight = primaryStage.getHeight();

                // Tạo Scene mới với cùng kích thước
                Scene scene = new Scene(root, currentWidth, currentHeight);
                primaryStage.setScene(scene);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            showFatalAlert(
                    "Error loading interface",
                    "Cannot load " + fxmlPath + "\nDetails: " + e.getMessage()
            );
        }
    }

    /**
     * Load FXML với FXMLLoader để truy cập controller
     * Chuẩn để dùng khi cần tương tác với controller trước khi hiển thị scene
     * @param fxmlPath Đường dẫn tới FXML file
     * @return FXMLLoader đã load FXML, hoặc null nếu lỗi
     */
    public static FXMLLoader loadScreenWithController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ClientStart.class.getResource(fxmlPath),
                            "Cannot find " + fxmlPath)
            );
            loader.load();
            return loader;
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            showFatalAlert(
                    "Error loading interface",
                    "Cannot load " + fxmlPath + "\nDetails: " + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Hiển thị scene với kích thước hiện tại của cửa sổ
     * Dùng khi đã load FXML rồi muốn hiển thị
     */
    public static void setSceneWithCurrentDimensions(Parent root) {
        if (primaryStage != null && root != null) {
            primaryStage.getScene().setRoot(root); //
        }
    }

    private static void showFatalAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Lấy primary stage (dùng cho các controller cần truy cập Stage)
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // ──────────────────────────────────────────────────────
    //  MAIN
    // ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}