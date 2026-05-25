/*
package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WatchListDao {

    private final Connection con;
    private static final Logger logger = Logger.getLogger(WatchListDao.class.getName());

    public WatchListDao() {
        Connection fake = null;
        try {
            fake = DatabaseConnection.getInstance().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE,"Error connecting to the database in WatchDao: ",e);
        }
        this.con = fake;
    }

    // Lưu watch vào DB
    public void watch(String userId, String auctionId){
        String sql = "INSERT IGNORE INTO user_watched_auctions (user_id, auction_id) VALUES (?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error saving watch record: " + e.getMessage());
        }
    }

    // Xóa watch khỏi DB
    public void unwatch(String userId, String auctionId){
        String sql = "DELETE FROM user_watched_auctions WHERE user_id = ? AND auction_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting watch record: " + e.getMessage());
        }
    }

    // Lấy danh sách auctionId mà user đang watch — dùng khi login lại
    public List<String> findWatchedAuctionIds(String userId){
        String sql = "SELECT auction_id FROM user_watched_auctions WHERE user_id = ?";
        List<String> result = new ArrayList<>();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("auction_id"));
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error finding watched auctions: " + e.getMessage());
        }
        return result;
    }

    // Kiểm tra user có đang watch auction không
    public boolean isWatching(String userId, String auctionId){
        String sql = "SELECT COUNT(*) FROM user_watched_auctions WHERE user_id = ? AND auction_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error checking watch record: " + e.getMessage());
        }
        return false;
    }

    /// unwatch() chính là delete của WatchList, không cần thêm.

    */
/*
     * Hard delete toàn bộ watch record của một auction.
     * Gọi khi auction bị xóa hoặc cancel — dọn dẹp orphan records.
     */
/*

    public void deleteByAuctionId(String auctionId){
        String sql = "DELETE FROM user_watched_auctions WHERE auction_id = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting watch records for auction " + auctionId + ": " + e.getMessage());
        }
    }

    */
/*
     * Hard delete toàn bộ watch record của một user.
     * Dùng khi user bị ban vĩnh viễn hoặc xóa tài khoản.
     */
/*

    public void deleteByUserId(String userId){
        String sql = "DELETE FROM user_watched_auctions WHERE user_id = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting watch records for user " + userId + ": " + e.getMessage());
        }
    }
}*/
package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO quản lý danh sách theo dõi (WatchList) của user.
 * Đã được điều chỉnh để tương thích hoàn toàn với SQLite.
 */
public class WatchListDao {

    private final Connection con;
    private static final Logger logger = Logger.getLogger(WatchListDao.class.getName());

    public WatchListDao() {
        this.con = null;
    }
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // Lưu watch vào DB - SQLite dùng INSERT OR IGNORE
    public void watch(String userId, String auctionId) {
        String sql = "INSERT OR IGNORE INTO user_watched_auctions (user_id, auction_id) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error saving watch record: " + e.getMessage());
        }
    }

    // Xóa watch khỏi DB
    public void unwatch(String userId, String auctionId) {
        String sql = "DELETE FROM user_watched_auctions WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting watch record: " + e.getMessage());
        }
    }

    // Lấy danh sách auctionId mà user đang watch
    public List<String> findWatchedAuctionIds(String userId) {
        String sql = "SELECT auction_id FROM user_watched_auctions WHERE user_id = ?";
        List<String> result = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("auction_id"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding watched auctions: " + e.getMessage());
        }
        return result;
    }

    // Kiểm tra user có đang watch auction không
    public boolean isWatching(String userId, String auctionId) {
        String sql = "SELECT COUNT(*) FROM user_watched_auctions WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking watch record: " + e.getMessage());
        }
        return false;
    }

    /**
     * Hard delete toàn bộ watch record của một auction.
     */
    public void deleteByAuctionId(String auctionId) {
        String sql = "DELETE FROM user_watched_auctions WHERE auction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting watch records for auction " + auctionId + ": " + e.getMessage());
        }
    }

    /**
     * Hard delete toàn bộ watch record của một user.
     */
    public void deleteByUserId(String userId) {
        String sql = "DELETE FROM user_watched_auctions WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting watch records for user " + userId + ": " + e.getMessage());
        }
    }
}