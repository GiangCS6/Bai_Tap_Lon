package bai_tap_lon.common.model.item;

import bai_tap_lon.common.model.entity.BidTransaction;
import bai_tap_lon.common.model.entity.Entity;
import bai_tap_lon.common.model.user.Seller;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

/**
 * Abstract Item — base class cho mọi loại sản phẩm đấu giá.
 *
 * Schema mới (sau migration sang JSON column):
 *   items: id, item_name, description, starting_price, image_Url, category,
 *          create_at, seller_id, attributes JSON
 *
 * Mỗi subclass tự override 2 method:
 *   - getCategory()    : tên category (khớp với key trong ItemFactory)
 *   - getAttributes()  : các thuộc tính riêng dạng JsonObject để lưu vào cột `attributes`
 *
 * Việc rebuild Item từ JSON khi đọc DB do ItemFactory đảm nhiệm.
 *
 * Thêm category mới = thêm 1 file subclass + 1 dòng register, KHÔNG đụng schema,
 * KHÔNG đụng ItemDAO, KHÔNG đụng Item.java này.
 */
public abstract class Item extends Entity {

    protected String name;
    protected String description;
    protected long startingPrice;
    protected final Seller seller;
    protected String imageUrl;

    private static final Logger logger = Logger.getLogger(Item.class.getName());


    public Item(String name, String description,
                long startingPrice, Seller seller, String imageUrl) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.seller = seller;
        this.imageUrl = imageUrl;
    }

    public boolean validate() {
        return name != null && !name.trim().isEmpty()
                && description != null
                && startingPrice > 0
                && seller != null;
    }

    /** Tên category — phải khớp với key đã đăng ký trong ItemFactory. */
    public abstract String getCategory();

    /** Thuộc tính riêng dạng JSON — vừa để gửi qua mạng vừa để lưu DB. */
    public abstract JsonObject getAttributes();

    @Override
    public void printInfo() {
        logger.info("Item ID: " + getId());
        logger.info("Name: " + getName());
        logger.info("Description: " + getDescription());
        logger.info("Starting Price: " + getStartingPrice());
        logger.info("Seller: " + getSeller());
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getStartingPrice() { return startingPrice; }
    public Seller getSeller() { return seller; }
    public String getImageUrl() { return imageUrl; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}