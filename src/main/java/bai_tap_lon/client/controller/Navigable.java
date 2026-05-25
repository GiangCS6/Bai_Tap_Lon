package bai_tap_lon.client.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Controller nào muốn nhận navigation context (stage + previous scene)
 * thì implement interface này.
 *
 * Thay thế cho cách cũ dùng reflection:
 *   ctrl.getClass().getMethod("setNavigationContext", ...).invoke(ctrl, ...)
 *
 * Cách dùng trong switchScene():
 *   if (ctrl instanceof Navigable navigable) {
 *       navigable.setNavigationContext(stage, previousScene);
 *   }
 */
public interface Navigable {
    void setNavigationContext(Stage stage, Scene previousScene);
}
