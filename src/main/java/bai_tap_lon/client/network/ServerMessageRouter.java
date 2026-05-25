package bai_tap_lon.client.network;

import bai_tap_lon.common.network.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ServerMessageRouter {

    public static final Gson gson = new Gson();
    private static final Map<String, Consumer<JsonObject>> successHandlers = new ConcurrentHashMap<>();
    private static final Map<String, BiConsumer<String,String>> errorHandlers = new ConcurrentHashMap<>();
    private static final Map<String, List<Consumer<JsonObject>>> pushListener = new ConcurrentHashMap<>();

    public static void register(String action,
                                Consumer<JsonObject> onSuccess,
                                BiConsumer<String,String> onError) {
        successHandlers.put(action, onSuccess);
        errorHandlers.put(action, onError);
    }

    public static void route(JsonObject object) {

        if (!object.has("isPushEvent")) {
            Response response = gson.fromJson(object, Response.class);
            responseRoute(response);
        } else {
            Push push = gson.fromJson(object, Push.class);
            pushRoute(push);
        }
    }

    public static void responseRoute(Response response) {
        String action = response.getAction();
        JsonObject data = response.getData();
        String error = response.getError();
        boolean success = response.isSuccess();
        String errorMessage = response.getErrorMessage();

        if (success) {
            Consumer<JsonObject> successHandler = successHandlers.remove(action); // remove: dùng 1 lần
            if (successHandler != null) {
                Platform.runLater(() -> successHandler.accept(data));
            }
        }
        else {
            BiConsumer<String,String> errorHandler = errorHandlers.remove(action);
            if (errorHandler != null){
                Platform.runLater(() -> errorHandler.accept(error, errorMessage));
            }
        }

    }

    public static void pushRoute(Push push) {
        String action = push.getAction();
        JsonObject data = push.getData();
        // ── Push ──

        List<Consumer<JsonObject>> handlers = pushListener.get(push.getAction());
        if (handlers == null) return;
        for (Consumer<JsonObject> handler : handlers) {
            Platform.runLater(() -> handler.accept(data));
        }
    }

    /// ___PUSH SUBSCRIBE____
    public static void subscribePush(String action, Consumer<JsonObject> handler){
        pushListener.computeIfAbsent(action, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    public static void unsubscribePush(String action, Consumer<JsonObject> handler){
        pushListener.computeIfPresent(action, (k,v) -> {
            v.remove(handler);
            return v;
        });
    }
}
