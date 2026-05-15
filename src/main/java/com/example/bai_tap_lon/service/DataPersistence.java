package com.example.bai_tap_lon.service;

import com.example.bai_tap_lon.model.Admin;
import com.example.bai_tap_lon.model.AuctionItem;
import com.example.bai_tap_lon.model.Bidder;
import com.example.bai_tap_lon.model.Seller;
import com.example.bai_tap_lon.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataPersistence {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.json";
    private static final String ITEMS_FILE = DATA_DIR + "/items.json";
    private static final String METADATA_FILE = DATA_DIR + "/metadata.json";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(User.class, new UserSerializer())
            .create();

    static {
        // Tạo thư mục data nếu chưa tồn tại
        new File(DATA_DIR).mkdirs();
    }

    /**
     * Lưu danh sách người dùng vào file
     */
    public static void saveUsers(List<User> users, int nextUserId) throws IOException {
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            JsonObject json = new JsonObject();
            Type userListType = new TypeToken<List<User>>(){}.getType();
            json.add("users", gson.toJsonTree(users, userListType));
            json.addProperty("nextUserId", nextUserId);
            gson.toJson(json, writer);
        }
    }

    /**
     * Tải danh sách người dùng từ file
     */
    public static UserLoadData loadUsers() throws IOException {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return new UserLoadData(new ArrayList<>(), 1);
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            List<User> users = new ArrayList<>();

            if (json.has("users")) {
                // Parse users with type discrimination
                for (JsonElement element : json.getAsJsonArray("users")) {
                    JsonObject userJson = element.getAsJsonObject();
                    String type = readUserType(userJson);
                    User user = parseUser(userJson, type);
                    users.add(user);
                }
            }

            int nextUserId = json.has("nextUserId") ? json.get("nextUserId").getAsInt() : 1;
            return new UserLoadData(users, nextUserId);
        }
    }

    /**
     * Lưu danh sách items vào file
     */
    public static void saveItems(List<AuctionItem> items, int nextItemId) throws IOException {
        try (FileWriter writer = new FileWriter(ITEMS_FILE)) {
            JsonObject json = new JsonObject();
            json.add("items", gson.toJsonTree(items));
            json.addProperty("nextItemId", nextItemId);
            gson.toJson(json, writer);
        }
    }

    /**
     * Tải danh sách items từ file
     */
    public static ItemLoadData loadItems(List<User> users) throws IOException {
        File file = new File(ITEMS_FILE);
        if (!file.exists()) {
            return new ItemLoadData(new ArrayList<>(), 1);
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            Type itemType = new TypeToken<List<AuctionItem>>(){}.getType();
            List<AuctionItem> items = gson.fromJson(json.getAsJsonArray("items"), itemType);

            if (items == null) {
                items = new ArrayList<>();
            }

            int nextItemId = json.has("nextItemId") ? json.get("nextItemId").getAsInt() : 1;
            return new ItemLoadData(items, nextItemId);
        }
    }

    private static String readUserType(JsonObject userJson) {
        JsonElement type = userJson.get("type");
        if (type == null || type.isJsonNull()) {
            type = userJson.get("role");
        }
        if (type == null || type.isJsonNull()) {
            throw new IllegalArgumentException("Missing user type");
        }
        return type.getAsString();
    }

    private static User parseUser(JsonObject userJson, String type) {
        int id = userJson.get("id").getAsInt();
        String username = userJson.get("username").getAsString();
        String password = userJson.get("password").getAsString();
        String fullName = userJson.get("fullName").getAsString();
        boolean locked = userJson.has("locked") && userJson.get("locked").getAsBoolean();

        return switch (type) {
            case "ADMIN" -> new Admin(id, username, password, fullName, locked);
            case "SELLER" -> new Seller(id, username, password, fullName, locked);
            case "BIDDER" -> new Bidder(id, username, password, fullName, locked);
            default -> throw new IllegalArgumentException("Unknown user type: " + type);
        };
    }

    /**
     * Xóa tất cả dữ liệu (dùng để reset)
     */
    public static void clearData() throws IOException {
        Files.deleteIfExists(Paths.get(USERS_FILE));
        Files.deleteIfExists(Paths.get(ITEMS_FILE));
        Files.deleteIfExists(Paths.get(METADATA_FILE));
    }

    /**
     * Lớp helper để trả về dữ liệu users khi load
     */
    public static class UserLoadData {
        public final List<User> users;
        public final int nextUserId;

        public UserLoadData(List<User> users, int nextUserId) {
            this.users = users;
            this.nextUserId = nextUserId;
        }
    }

    /**
     * Lớp helper để trả về dữ liệu items khi load
     */
    public static class ItemLoadData {
        public final List<AuctionItem> items;
        public final int nextItemId;

        public ItemLoadData(List<AuctionItem> items, int nextItemId) {
            this.items = items;
            this.nextItemId = nextItemId;
        }
    }
}
