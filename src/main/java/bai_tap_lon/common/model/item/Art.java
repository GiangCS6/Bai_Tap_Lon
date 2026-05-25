package bai_tap_lon.common.model.item;

import bai_tap_lon.common.model.user.Seller;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

public class Art extends Item {

    public static final String CATEGORY = "Art";

    private final String artist;
    private final String yearCreated;

    private static final Logger logger = Logger.getLogger(Art.class.getName());


    // ─── Self-registration ──────────────────────────────────────────
    static {
        ItemFactory.register(CATEGORY, (name, desc, price, seller, imageUrl, attrs) ->
                new Art(
                        name, desc, price, seller, imageUrl,
                        getString(attrs, "artist", ""),
                        getString(attrs, "yearCreated", "")
                ));
    }
    // ────────────────────────────────────────────────────────────────

    public Art(String name, String description, long startingPrice, Seller seller, String imageUrl,
               String artist, String yearCreated) {
        super(name, description, startingPrice, seller, imageUrl);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public JsonObject getAttributes() {
        JsonObject attrs = new JsonObject();
        attrs.addProperty("artist", artist);
        attrs.addProperty("yearCreated", yearCreated);
        return attrs;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        logger.info("Artist: " + artist);
        logger.info("Year Created: " + yearCreated);
    }

    public String getArtist() { return artist; }
    public String getYearCreated() { return yearCreated; }

    // ─── Helper đọc JsonObject an toàn ──────────────────────────────
    private static String getString(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }
}