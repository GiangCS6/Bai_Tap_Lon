package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.Client;
import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.client.service.ImageUploadService;
import bai_tap_lon.common.model.user.Seller;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller cho AddItem.fxml.
 *
 * Flow:
 *   1. User nhập đủ thông tin → nhấn "Post Item"
 *   2. onSubmit() validate → đóng gói JsonObject payload → build Request → gửi server
 *   3. Server xử lý DB xong → gọi lại onAddItemSuccess(JsonObject) trên luồng network
 *   4. onAddItemSuccess() chạy Platform.runLater → thêm item vào SellerHistory → quay về màn trước
 *
 * Payload theo spec POST_ITEM:
 *   { name, description, startingPrice, category (UPPER_SNAKE), attributes, auctionDurationMinutes, startTime, endTime, imageUrl }
 */
public class AddItemController implements Initializable {

    // ══════════════════════════════════════════════════════
    //  FXML BINDINGS
    // ══════════════════════════════════════════════════════

    /* ── Basic Information ── */
    @FXML private TextField           fieldName;
    @FXML private ComboBox<String>    comboCategory;
    @FXML private TextArea            fieldDescription;

    /* ── Image Upload ── */
    @FXML private StackPane           imageUploadZone;
    @FXML private VBox                uploadPlaceholder;
    @FXML private ImageView           imagePreview;
    @FXML private TextField           fieldImageUrl;
    @FXML private Button              btnRemoveImage;

    /* ── Category Panels ── */
    @FXML private VBox                panelElectronics;
    @FXML private TextField           fieldBrand;
    @FXML private TextField           fieldWarranty;

    @FXML private VBox                panelArt;
    @FXML private TextField           fieldArtist;
    @FXML private TextField           fieldManufactureYear;

    @FXML private VBox                panelVehicle;
    @FXML private TextField           fieldVehicleBrand;
    @FXML private TextField           fieldVehicleYear;

    /* ── Pricing & Auction Duration ── */
    @FXML private TextField           fieldStartingPrice;
    @FXML private TextField           fieldDurationMinutes;
    @FXML private DatePicker          pickerStartDate;
    @FXML private TextField           fieldStartTimeClock;

    /* ── Status / Actions ── */
    @FXML private Label               labelStatus;
    @FXML private Button              btnSubmit;

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════

    private File                      selectedImageFile;
    private Seller                    currentSeller;
    private String                    username = "";
    private String                    role = "";

    /** Tham chiếu về màn SellerHistory để thêm item sau khi server xác nhận thành công */
    private SellerHistoryController   sellerHistoryController;

    /** Stage + Scene trước đó để navigate back */
    private Stage                     stage;
    private Scene                     previousScene;
    private Parent previousRoot;
    private static final String ZONE_NORMAL =
            "-fx-background-color: #0A192F; -fx-border-color: #1E3A5F;" +
                    "-fx-border-style: dashed; -fx-border-width: 2;" +
                    "-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
    private static final String ZONE_HOVER =
            "-fx-background-color: #0D2240; -fx-border-color: #64FFDA;" +
                    "-fx-border-style: dashed; -fx-border-width: 2;" +
                    "-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";

        private static final DateTimeFormatter TIME_HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ImageUploadService imageUploadService = new ImageUploadService();

    // ══════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCategory();
        setupImageZoneHover();
        setupDefaultStartTime();
        resolveSellerFromSession();
    }

    /**
     * Gọi từ SellerHistoryController trước khi chuyển scene.
     */
    public void setData(Seller seller,
                        SellerHistoryController parent,
                        Stage stage,
                        Scene prev) {
        this.currentSeller           = seller;
        this.sellerHistoryController = parent;
        this.stage                   = stage;
        this.previousScene           = prev;
        this.previousRoot = (prev != null) ? (Parent) prev.getRoot() : null;
        resolveSellerFromSession();
    }

    // ══════════════════════════════════════════════════════
    //  SETUP HELPERS
    // ══════════════════════════════════════════════════════

    private void setupCategory() {
        comboCategory.getItems().addAll("Electronics", "Art", "Vehicle");
        comboCategory.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> onCategoryChanged(newVal));
    }

    private void setupImageZoneHover() {
        imageUploadZone.setOnMouseEntered(e -> imageUploadZone.setStyle(ZONE_HOVER));
        imageUploadZone.setOnMouseExited (e -> imageUploadZone.setStyle(ZONE_NORMAL));
    }

    private void setupDefaultStartTime() {
        LocalDateTime now = TimeUtil.now();
        if (pickerStartDate != null && pickerStartDate.getValue() == null) {
            pickerStartDate.setValue(now.toLocalDate());
        }
        if (fieldStartTimeClock != null && isBlank(fieldStartTimeClock.getText())) {
            fieldStartTimeClock.setText(now.toLocalTime().withNano(0).format(TIME_HMS));
        }
    }

    // ══════════════════════════════════════════════════════
    //  CATEGORY PANEL TOGGLE
    // ══════════════════════════════════════════════════════

    private void onCategoryChanged(String category) {
        setPanel(panelElectronics, "Electronics".equals(category));
        setPanel(panelArt,         "Art"        .equals(category));
        setPanel(panelVehicle,     "Vehicle"    .equals(category));
    }

    private void setPanel(VBox panel, boolean show) {
        panel.setVisible(show);
        panel.setManaged(show);
    }

    // ══════════════════════════════════════════════════════
    //  IMAGE UPLOAD
    // ══════════════════════════════════════════════════════

    @FXML
    private void onChooseImageMouse(MouseEvent e) { openFileChooser(); }

    @FXML
    private void onChooseImageAction(ActionEvent e) { openFileChooser(); }

    private void openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Item Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = chooser.showOpenDialog(imageUploadZone.getScene().getWindow());
        if (file != null) loadImageFile(file);
    }

    @FXML
    private void onImageDragOver(DragEvent e) {
        if (e.getDragboard().hasFiles()) {
            e.acceptTransferModes(TransferMode.COPY);
            imageUploadZone.setStyle(ZONE_HOVER);
        }
        e.consume();
    }

    @FXML
    private void onImageDragDropped(DragEvent e) {
        imageUploadZone.setStyle(ZONE_NORMAL);
        List<File> files = e.getDragboard().getFiles();
        if (!files.isEmpty()) loadImageFile(files.get(0));
        e.setDropCompleted(true);
        e.consume();
    }

    @FXML
    private void onRemoveImage(ActionEvent e) {
        selectedImageFile = null;
        imagePreview.setImage(null);
        showPreview(false);
        fieldImageUrl.clear();
    }

    private void loadImageFile(File file) {
        if (file.length() > 10L * 1024 * 1024) {
            showError("Image file is too large (max 10 MB).");
            return;
        }
        selectedImageFile = file;
        imagePreview.setImage(new Image(file.toURI().toString(), true));
        showPreview(true);
        fieldImageUrl.setText(file.getAbsolutePath());
        clearError();
    }

    private void showPreview(boolean show) {
        imagePreview.setVisible(show);
        imagePreview.setManaged(show);
        uploadPlaceholder.setVisible(!show);
        uploadPlaceholder.setManaged(!show);
        btnRemoveImage.setVisible(show);
        btnRemoveImage.setManaged(show);
    }

    // ══════════════════════════════════════════════════════
    //  SUBMIT — validate → đóng gói Request → gửi server
    // ══════════════════════════════════════════════════════

    @FXML
    private void onSubmit(ActionEvent e) {
        clearError();
        resolveSellerFromSession();

        if (currentSeller == null) {
            showError("Please login as a seller before posting an item.");
            return;
        }

        // ── 1. Validate các trường bắt buộc ──────────────
        if (isBlank(fieldName.getText())) {
            showError("Item name is required.");
            fieldName.requestFocus();
            return;
        }
        if (comboCategory.getValue() == null) {
            showError("Please select a category.");
            return;
        }

        long startingPrice;
        try {
            startingPrice = Long.parseLong(fieldStartingPrice.getText().trim());
            if (startingPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Starting price must be a positive whole number (e.g., 500000).");
            fieldStartingPrice.requestFocus();
            return;
        }

        // ── auctionDurationMinutes: min=5, max=10080 (7 ngày) ────────────
        int durationMinutes;
        try {
            durationMinutes = Integer.parseInt(fieldDurationMinutes.getText().trim());
            if (durationMinutes < 5 || durationMinutes > 10080) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Auction duration must be between 5 and 10080 minutes (7 days).");
            fieldDurationMinutes.requestFocus();
            return;
        }

        if (pickerStartDate.getValue() == null) {
            showError("Please choose start date.");
            pickerStartDate.requestFocus();
            return;
        }

        LocalTime selectedTime;
        try {
            selectedTime = LocalTime.parse(fieldStartTimeClock.getText().trim(), TIME_HMS);
        } catch (DateTimeParseException ex) {
            showError("Start time must be in HH:mm:ss format (e.g. 09:30:00).");
            fieldStartTimeClock.requestFocus();
            return;
        }

        LocalDate selectedDate = pickerStartDate.getValue();
        LocalDateTime normalizedStartLdt = LocalDateTime.of(selectedDate, selectedTime);
        String normalizedStartTime = TimeUtil.toIso(normalizedStartLdt);
        String normalizedEndTime = TimeUtil.toIso(normalizedStartLdt.plusMinutes(durationMinutes));

        // ── 2. Validate theo category ─────────────────────
        String category = comboCategory.getValue();
        int currentYear = TimeUtil.now().getYear();

        int warrantyMonths  = 0;
        int manufactureYear = 0;

        switch (category) {
            case "Electronics" -> {
                if (!isBlank(fieldWarranty.getText())) {
                    try {
                        warrantyMonths = Integer.parseInt(fieldWarranty.getText().trim());
                    } catch (NumberFormatException ex) {
                        showError("Warranty must be a whole number (months).");
                        return;
                    }
                }
            }
            case "Art" -> {
                if (!isBlank(fieldManufactureYear.getText())) {
                    try {
                        manufactureYear = Integer.parseInt(fieldManufactureYear.getText().trim());
                        if (manufactureYear > currentYear) {
                            showError("Art year must be less than the current year.");
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        showError("Manufacture year must be a valid number.");
                        return;
                    }
                }
            }
            case "Vehicle" -> {
                if (!isBlank(fieldVehicleYear.getText())) {
                    try {
                        manufactureYear = Integer.parseInt(fieldVehicleYear.getText().trim());
                        if (manufactureYear > currentYear) {
                            showError("Vehicle year must be less than the current year.");
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        showError("Manufacture year must be a valid number.");
                        return;
                    }
                }
            }
        }

        // ── 3. Đóng gói payload theo spec POST_ITEM ───────
        String categoryEnum = category.toUpperCase();

        JsonObject attributes = new JsonObject();
        if (selectedImageFile == null || isBlank(fieldImageUrl.getText())) {
            showError("Please select an image for the item.");
            imageUploadZone.requestFocus();
            return;
        } else if (category.equals("Electronics")) {
            attributes.addProperty("brand", fieldBrand.getText().trim());
            attributes.addProperty("warrantyMonths", warrantyMonths);
        } else if (category.equals("Art")) {
            attributes.addProperty("artist", fieldArtist.getText().trim());
            if (manufactureYear > 0) attributes.addProperty("yearCreated", String.valueOf(manufactureYear));
        } else if (category.equals("Vehicle")) {
            attributes.addProperty("brand", fieldVehicleBrand.getText().trim());
            if (manufactureYear > 0) attributes.addProperty("year", manufactureYear);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("name",                   fieldName.getText().trim());
        payload.addProperty("description",            fieldDescription.getText().trim());
        payload.addProperty("startingPrice",          startingPrice);
        payload.addProperty("category",               categoryEnum);
        payload.addProperty("auctionDurationMinutes", durationMinutes);
        payload.addProperty("startTime",              normalizedStartTime);
        payload.addProperty("endTime",                normalizedEndTime);
        payload.addProperty("imageUrl",               fieldImageUrl.getText().trim());
        payload.add       ("attributes",              attributes);

        btnSubmit.setDisable(true);
        showStatus("Uploading image...", "#64FFDA");

        imageUploadService.uploadAsync(selectedImageFile)
                .thenAccept(imageUrl -> {
                    payload.addProperty("imageUrl", imageUrl);

                    Request request = new Request.Builder()
                            .action("POST_ITEM")
                            .payload(payload)
                            .build();

                    ServerMessageRouter.register("POST_ITEM",
                            this::onAddItemSuccess,
                            this::onAddItemFail);

                    showStatus("Posting item...", "#64FFDA");
                    Client.getInstance().sendRequest(request);
                })
                .exceptionally(ex -> {
                    btnSubmit.setDisable(false);
                    showError("Upload ảnh thất bại: " + ex.getMessage());
                    return null;
                });
    }

    // ══════════════════════════════════════════════════════
    //  CALLBACKS — server gọi về sau khi xử lý xong
    // ══════════════════════════════════════════════════════

    public void onAddItemSuccess(JsonObject responseData) {
        String auctionId = "";
        if (responseData != null && responseData.has("auctionId") && !responseData.get("auctionId").isJsonNull()) {
            try {
                auctionId = responseData.get("auctionId").getAsString();
            } catch (UnsupportedOperationException ex) {
                Platform.runLater(() -> {
                    showError("Invalid auctionId returned from server.");
                    btnSubmit.setDisable(false);
                });
                return;
            }
        }

//        Platform.runLater(() -> {
            if (sellerHistoryController != null) {
                // Reload from server so table data always comes from DB-backed DTO.
                sellerHistoryController.getMyItems();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Item posted successfully!");
                alert.showAndWait();

                navigateBack();
                return;
            }
            openSellerHistoryScreen();
//        });
    }

    public void onAddItemFail(String errorCode,String errorMessage) {
            String safeMessage = (errorMessage == null || errorMessage.isBlank())
                    ? "Unknown server error."
                    : errorMessage;
            btnSubmit.setDisable(false);
            showError("Server error: " + safeMessage);
            showAlert(Alert.AlertType.ERROR, errorCode, safeMessage);
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════

    @FXML
    private void onBack(ActionEvent e) {
        returnToSellerHistoryScreen();
    }

    private void navigateBack() {
        returnToSellerHistoryScreen();
    }

    private void openSellerHistoryScreen() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewfxml/SellerHistory.fxml"));
        Parent root = loader.load();
        Stage targetStage = stage != null
                ? stage
                : (fieldName.getScene() != null ? (Stage) fieldName.getScene().getWindow() : null);
        if (targetStage != null) {
            targetStage.setScene(new Scene(root, targetStage.getWidth(), targetStage.getHeight()));
            targetStage.show();
        }
    } catch (IOException ex) {
        ex.printStackTrace();
    }
}
    private void returnToSellerHistoryScreen() {
        Stage targetStage = stage != null
                ? stage
                : (fieldName.getScene() != null ? (Stage) fieldName.getScene().getWindow() : null);
        if (targetStage == null) return;

        if (previousRoot != null && previousScene != null) {
            previousScene.setRoot(previousRoot);
            targetStage.setScene(previousScene);
            targetStage.setMaximized(true);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewfxml/SellerHistory.fxml"));
            Parent root = loader.load();
            targetStage.getScene().setRoot(root);
            targetStage.setMaximized(true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void resolveSellerFromSession() {
        if (currentSeller != null) {
            return;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getUsername() == null ? "" : currentUser.getUsername();
            role = currentUser.getRole() == null ? "" : currentUser.getRole();
        }
        if (currentUser instanceof Seller seller) {
            currentSeller = seller;
        }
    }

    // ══════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════

    private void showError(String msg) {
        showStatus("⚠  " + msg, "#FF6B6B");
    }

    private void showStatus(String msg, String color) {
        labelStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        labelStatus.setText(msg);
    }

    private void clearError() {
        labelStatus.setText("");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
