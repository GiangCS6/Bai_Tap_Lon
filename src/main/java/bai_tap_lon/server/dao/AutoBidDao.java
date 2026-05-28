/*
package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.model.entity.AutoBidSetting;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/*
 * DAO cho bảng auto_bid_settings.
 * Mỗi row = 1 bidder đang bật autobid cho 1 auction.
 * UNIQUE KEY (bidder_id, auction_id) — upsert bằng ON DUPLICATE KEY UPDATE.
 */
/*

public class AutoBidDao {

    private final Connection con;
    private static final Logger logger = Logger.getLogger(AutoBidDao.class.getName());

    public AutoBidDao() {
        Connection fake = null;
        try {
            fake = DatabaseConnection.getInstance().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE,"Error connecting to database in AutoBidDao: " ,e);
        }
        this.con = fake;
    }

    // ── Upsert: nếu đã có thì update, chưa có thì insert ──────────────
    public void save(String bidderId, String auctionId, long maxBid, long increment) {
        String sql =
                "INSERT INTO auto_bid_settings (bidder_id, auction_id, max_bid, increment, is_active) " +
                        "VALUES (?, ?, ?, ?, 1) " +
                        "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), increment = VALUES(increment), is_active = 1";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, bidderId);
            stmt.setString(2, auctionId);
            stmt.setLong(3, maxBid);
            stmt.setLong(4, increment);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error saving auto bid setting: " + e.getMessage());
        }
    }

    // ── Deactivate khi user cancel autobid ────────────────────────────
    public void deactivate(String bidderId, String auctionId) {
        String sql = "UPDATE auto_bid_settings SET is_active = 0 WHERE bidder_id = ? AND auction_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, bidderId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deactivating auto bid setting: " + e.getMessage());
        }
    }

    // ── Load setting của 1 bidder trong 1 auction (null nếu không có) ──
    public AutoBidSetting findByBidderAndAuction(String bidderId, String auctionId) {
        String sql =
                "SELECT max_bid, increment FROM auto_bid_settings " +
                        "WHERE bidder_id = ? AND auction_id = ? AND is_active = 1";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, bidderId);
            stmt.setString(2, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new AutoBidSetting(rs.getLong("max_bid"), rs.getLong("increment"));
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error loading auto bid setting: " + e.getMessage());
        }
    }

    // ── Load tất cả bidder đang active autobid trong 1 auction ─────────
    public List<ActiveAutoBid> findActiveByAuction(String auctionId) {
        String sql =
                "SELECT bidder_id, max_bid, increment FROM auto_bid_settings " +
                        "WHERE auction_id = ? AND is_active = 1";
        List<ActiveAutoBid> result = new ArrayList<>();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new ActiveAutoBid(
                        rs.getString("bidder_id"),
                        rs.getLong("max_bid"),
                        rs.getLong("increment")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error loading active auto bids for auction: " + e.getMessage());
        }
        return result;
    }

    // ── Deactivate toàn bộ khi auction kết thúc ────────────────────────
    public void deactivateAllForAuction(String auctionId) {
        String sql = "UPDATE auto_bid_settings SET is_active = 0 WHERE auction_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deactivating all auto bids for auction: " + e.getMessage());
        }
    }

    // ── DTO nội bộ ─────────────────────────────────────────────────────
    public static class ActiveAutoBid {
        public final String bidderId;
        public final long maxBid;
        public final long increment;

        public ActiveAutoBid(String bidderId, long maxBid, long increment) {
            this.bidderId = bidderId;
            this.maxBid = maxBid;
            this.increment = increment;
        }
    }
}*/
package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.model.entity.AutoBidSetting;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * DAO cho bảng auto_bid_settings.
 * Mỗi row = 1 bidder đang bật autobid cho 1 auction.
 * UNIQUE (bidder_id, auction_id) — dùng ON CONFLICT cho SQLite.
 */
public class AutoBidDao {

    private final Connection con;
    private static final Logger logger = Logger.getLogger(AutoBidDao.class.getName());

    public AutoBidDao() {
        this.con = null;
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── Upsert: nếu đã có thì update, chưa có thì insert (SQLite syntax) ──────────────
    public void save(String bidderId, String auctionId, long maxBid, long increment) {
        String sql =
                "INSERT INTO auto_bid_settings " +
                        "(id, bidder_id, auction_id, max_bid, increment, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, 1) " +
                        "ON CONFLICT(bidder_id, auction_id) " +
                        "DO UPDATE SET " +
                        "max_bid = excluded.max_bid, " +
                        "increment = excluded.increment, " +
                        "is_active = 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, bidderId);
            stmt.setString(3, auctionId);
            stmt.setLong(4, maxBid);
            stmt.setLong(5, increment);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error saving auto bid setting: " + e.getMessage());
        }
    }

    // ── Deactivate khi user cancel autobid ────────────────────────────
    public void deactivate(String bidderId, String auctionId) {
        String sql = "UPDATE auto_bid_settings SET is_active = 0 WHERE bidder_id = ? AND auction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bidderId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deactivating auto bid setting: " + e.getMessage());
        }
    }

    // ── Load setting của 1 bidder trong 1 auction (null nếu không có) ──
    public AutoBidSetting findByBidderAndAuction(String bidderId, String auctionId) {
        String sql =
                "SELECT max_bid, increment FROM auto_bid_settings " +
                        "WHERE bidder_id = ? AND auction_id = ? AND is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bidderId);
            stmt.setString(2, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new AutoBidSetting(rs.getLong("max_bid"), rs.getLong("increment"));
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error loading auto bid setting: " + e.getMessage());
        }
    }

    // ── Load tất cả bidder đang active autobid trong 1 auction ─────────
    public List<ActiveAutoBid> findActiveByAuction(String auctionId) {
        String sql =
                "SELECT bidder_id, max_bid, increment FROM auto_bid_settings " +
                        "WHERE auction_id = ? AND is_active = 1";
        List<ActiveAutoBid> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new ActiveAutoBid(
                        rs.getString("bidder_id"),
                        rs.getLong("max_bid"),
                        rs.getLong("increment")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error loading active auto bids for auction: " + e.getMessage());
        }
        return result;
    }

    // ── Deactivate toàn bộ khi auction kết thúc ────────────────────────
    public void deactivateAllForAuction(String auctionId) {
        String sql = "UPDATE auto_bid_settings SET is_active = 0 WHERE auction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deactivating all auto bids for auction: " + e.getMessage());
        }
    }

    // ── DTO nội bộ ─────────────────────────────────────────────────────
    public static class ActiveAutoBid {
        public final String bidderId;
        public final long maxBid;
        public final long increment;

        public ActiveAutoBid(String bidderId, long maxBid, long increment) {
            this.bidderId = bidderId;
            this.maxBid = maxBid;
            this.increment = increment;
        }
    }
}