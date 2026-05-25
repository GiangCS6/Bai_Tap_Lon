package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.model.entity.AuctionStatus;
import bai_tap_lon.common.model.item.Item;
import bai_tap_lon.common.model.item.ItemFactory;
import bai_tap_lon.common.model.user.Seller;
import bai_tap_lon.server.dto.ItemDetails;
import bai_tap_lon.server.dto.ItemInfoDetails;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO cho bảng items (sau khi migrate sang JSON column).
 *
 * Schema mới:
 *   items: id, item_name, description, starting_price, image_Url, category,
 *          create_at, seller_id, attributes JSON
 *
 * So với version trước:
 *   - INSERT giảm từ 11 cột → 8 cột
 *   - KHÔNG còn bind 4 cột phụ thuộc category
 *   - KHÔNG còn if/else theo category khi đọc DB
 *   - Thêm category mới chỉ cần thêm 1 file subclass — KHÔNG đụng DAO, KHÔNG đụng schema
 *
 * Lưu ý:
 *  - image_Url viết hoa giữa chừng (đúng schema)
 *  - create_at có DEFAULT CURRENT_TIMESTAMP → không insert thủ công
 *  - Bảng items KHÔNG có cột is_deleted → không filter soft-delete
 */
public class ItemDAO {
    private final Connection connection;
    private static final Logger logger = Logger.getLogger(ItemDAO.class.getName());

    public ItemDAO() {
        this.connection = null;
    }
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SAVE
    // ═══════════════════════════════════════════════════════════════

    public boolean save(Item item) {
        if (item == null || !item.validate()) {
            throw new DatabaseException("Invalid item — cannot save");
        }
        if (!ItemFactory.isValidCategory(item.getCategory())) {
            throw new DatabaseException("Unknown category: " + item.getCategory());
        }
        if (item.getId() == null || item.getId().trim().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }
        if (item.getSeller() == null || item.getSeller().getId() == null) {
            throw new DatabaseException("Item must have a seller with valid id");
        }

        // Chỉ 8 placeholder — không còn brand/warranty/artist/year riêng
        String sql = "INSERT INTO items " +
                "(id, item_name, description, starting_price, image_Url, category, seller_id, attributes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setLong(4, item.getStartingPrice());
            stmt.setString(5, item.getImageUrl());
            stmt.setString(6, item.getCategory().toUpperCase());
            stmt.setString(7, item.getSeller().getId());

            // Cột JSON — Gson serialize JsonObject thành chuỗi
            JsonObject attrs = item.getAttributes();
            stmt.setString(8, attrs != null ? attrs.toString() : "{}");

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error saving item: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FIND
    // ═══════════════════════════════════════════════════════════════

    public Item findById(String id) {
        String sql = "SELECT i.*, s.username AS seller_name, s.email AS seller_email " +
                "FROM items i LEFT JOIN users s ON i.seller_id = s.id " +
                "WHERE i.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToItem(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding item by ID: " + e.getMessage());
        }
        return null;
    }

    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT i.*, s.username AS seller_name, s.email AS seller_email " +
                "FROM items i LEFT JOIN users s ON i.seller_id = s.id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Item item = mapResultSetToItem(rs);
                if (item != null) items.add(item);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all items: " + e.getMessage());
        }
        return items;
    }

    public List<ItemDetails> findBySellerId(String sellerId)  {
        // Bỏ filter i.is_deleted vì schema không có cột này
        String sql = """
            SELECT
                i.id            AS item_id,
                i.item_name,
                i.starting_price,
                i.category,
                a.id            AS auction_id,
                a.status        AS auction_status
            FROM items i
            JOIN auctions a ON a.item_id = i.id
            WHERE i.seller_id = ?
            ORDER BY a.start_time DESC
            """;

        List<ItemDetails> result = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new ItemDetails(
                            rs.getString("item_id"),
                            rs.getString("item_name"),
                            rs.getLong("starting_price"),
                            rs.getString("category"),
                            rs.getString("auction_id"),
                            AuctionStatus.valueOf(rs.getString("auction_status"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding items by seller ID: " + e.getMessage());
        }
        return result;
    }

    public ItemInfoDetails getItemDetailsByAuctionId(String auctionId) {
        String sql = """
            SELECT
                i.item_name,
                i.description,
                i.starting_price,
                i.image_Url,
                i.category,
                s.username AS seller_name,
                a.current_price,
                a.status
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            JOIN users s ON i.seller_id = s.id
            WHERE a.id = ? AND a.is_deleted = 0
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ItemInfoDetails(
                            rs.getString("item_name"),
                            rs.getString("description"),
                            rs.getString("image_Url"),
                            rs.getString("category"),
                            rs.getString("seller_name"),
                            rs.getLong("starting_price"),
                            rs.getLong("current_price"),
                            AuctionStatus.valueOf(rs.getString("status"))
                    );
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding item details by auction ID: " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MAPPING — đơn giản hẳn nhờ JSON column
    // ═══════════════════════════════════════════════════════════════

    private Item mapResultSetToItem(ResultSet rs) {
        try {
            String category = rs.getString("category");
            if (!ItemFactory.isValidCategory(category)) {
                return null; // skip row có category lạ
            }

            String id = rs.getString("id");
            String name = rs.getString("item_name");
            String description = rs.getString("description");
            long startingPrice = rs.getLong("starting_price");
            String imageUrl = rs.getString("image_Url");
            String sellerId = rs.getString("seller_id");

            // Build Seller "shallow" — đủ thông tin hiển thị
            String sellerName = rs.getString("seller_name");
            String sellerEmail = rs.getString("seller_email");
            Seller seller = new Seller(
                    sellerName != null ? sellerName : "N/A",
                    "",
                    sellerEmail != null ? sellerEmail : "N/A");
            seller.setId(sellerId);

            // Parse JSON attributes rồi đẩy vào factory — factory chọn đúng subclass
            JsonObject attrs = parseJsonSafe(rs.getString("attributes"));
            Item item = ItemFactory.createItem(category, name, description, startingPrice, seller, imageUrl, attrs);
            item.setId(id);
            item.setImageUrl(imageUrl);
            return item;
        } catch (SQLException e) {
            throw new DatabaseException("Error mapping ResultSet to Item: " + e.getMessage());
        }
    }

    /** Parse JSON an toàn — trả {} nếu null hoặc malformed thay vì throw. */
    private JsonObject parseJsonSafe(String json) {
        if (json == null || json.trim().isEmpty()) return new JsonObject();
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            logger.log(Level.SEVERE,"[ItemDAO] Malformed attributes JSON: " + json,e);
            return new JsonObject();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════════

    public boolean deleteById(String itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, itemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting item by ID: " + e.getMessage());
        }
    }
}