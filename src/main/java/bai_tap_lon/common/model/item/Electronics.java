package bai_tap_lon.common.model.item;

import bai_tap_lon.common.model.user.Seller;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

public class Electronics extends Item {

    public static final String CATEGORY = "Electronics";

    private final String brand;
    private final int warrantyMonths;

    private static final Logger logger = Logger.getLogger(Electronics.class.getName());


    // ─── Self-registration ──────────────────────────────────────────
    static {
        ItemFactory.register(CATEGORY, (name, desc, price, seller, imageUrl, attrs) ->
                new Electronics(
                        name, desc, price, seller, imageUrl,
                        getString(attrs, "brand", ""),
                        getInt(attrs, "warrantyMonths", 0)
                ));
    }
    // ────────────────────────────────────────────────────────────────

    public Electronics(String name, String description, long startingPrice, Seller seller, String imageUrl,
                       String brand, int warrantyMonths) {
        super(name, description, startingPrice, seller, imageUrl);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public JsonObject getAttributes() {
        JsonObject attrs = new JsonObject();
        attrs.addProperty("brand", brand);
        attrs.addProperty("warrantyMonths", warrantyMonths);
        return attrs;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        logger.info("Brand: " + brand);
        logger.info("Warranty Months: " + warrantyMonths);
    }

    public String getBrand() { return brand; }
    public int getWarrantyMonths() { return warrantyMonths; }

    // ─── Helpers đọc JsonObject an toàn ─────────────────────────────
    private static String getString(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }

    private static int getInt(JsonObject o, String key, int def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsInt() : def;
    }
}