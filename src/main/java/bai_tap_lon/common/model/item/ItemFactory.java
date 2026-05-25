package bai_tap_lon.common.model.item;

import bai_tap_lon.common.model.user.Seller;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ItemFactory — Registry pattern, làm việc trực tiếp với JsonObject.
 *
 * Lý do đơn giản hơn so với version cũ:
 *   - Schema dùng cột JSON → cả khi đọc DB lẫn nhận từ client đều là JsonObject
 *   - Không cần chuyển qua Map<String, Object> trung gian nữa
 *   - Không cần ItemRowMapper riêng — DAO tự parse JSON rồi gọi createItem
 *
 * Cách dùng:
 *   - RequestRouter:  ItemFactory.createItem(category, name, ..., attrs);
 *   - ItemDAO:        ItemFactory.createItem(category, name, ..., parsedAttrs);
 *
 * Thêm category mới (vd Book): tạo file Book.java + 1 dòng register trong static block.
 */
public final class ItemFactory {

    /** Builder dựng Item từ JsonObject attributes. */
    @FunctionalInterface
    public interface ItemBuilder {
        Item build(String name, String description, long startingPrice,
                   Seller seller, String itemUrl, JsonObject attributes);
    }

    private static final Map<String, ItemBuilder> BUILDERS = new ConcurrentHashMap<>();

    // Bảo đảm các subclass được nạp class → static block của chúng tự register
    static {
        try {
            Class.forName(Electronics.class.getName());
            Class.forName(Art.class.getName());
            Class.forName(Vehicle.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to register Item subclasses", e);
        }
    }

    private ItemFactory() {}

    /**
     * Đăng ký 1 category. Gọi từ static block của subclass.
     *
     * @param category   tên category, không phân biệt hoa/thường
     * @param builder    tạo Item từ JsonObject attributes
     */
    public static void register(String category, ItemBuilder builder) {
        if (category == null || builder == null) {
            throw new IllegalArgumentException("register() args must be non-null");
        }
        BUILDERS.put(category.toUpperCase(Locale.ROOT), builder);
    }

    /**
     * Tạo Item — 1 entry point duy nhất cho cả 2 path:
     *   - JSON từ client (RequestRouter)
     *   - JSON đọc từ DB (ItemDAO)
     */
    public static Item createItem(String category, String name, String description,
                                  long startingPrice, Seller seller, String itemUrl,
                                  JsonObject attributes) {
        ItemBuilder builder = BUILDERS.get(normalize(category));
        if (builder == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return builder.build(name, description, startingPrice, seller, itemUrl,
                attributes != null ? attributes : new JsonObject());
    }

    /**
     * Kiểm tra category có hợp lệ không (đã được register).
     * Dùng để validate input từ client trong RequestRouter.
     */
    public static boolean isValidCategory(String category) {
        return category != null && BUILDERS.containsKey(normalize(category));
    }

    private static String normalize(String category) {
        return category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
    }
}