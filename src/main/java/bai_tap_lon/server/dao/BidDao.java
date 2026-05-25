package bai_tap_lon.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException; //=>quản lý exception
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.model.entity.AuctionStatus;
import bai_tap_lon.common.model.entity.BidTransaction;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.network.TimeUtil;
import bai_tap_lon.server.dto.ActiveBidSummary;
import bai_tap_lon.server.dto.BidDetails;


public class BidDao {
    private final Connection con;// do trong databseconnection method getInstance thrown ngoại lệ => làm vậy để try - cacth
    private static final Logger logger = Logger.getLogger(BidDao.class.getName());
    public BidDao() {
        this.con=null;
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    //Save
    public void save(BidTransaction bt) {
        String sql = "insert into bids " +
                "(id, auction_id, bidder_id, amount, bid_time)" +
                "values (?,?,?,?,?) ";


        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bt.getId());
            stmt.setString(2, bt.getAuctionId());
            stmt.setString(3, bt.getBidder().getId());
            stmt.setLong(4, bt.getAmount());
            stmt.setString(5, TimeUtil.toIso(bt.getCreatedAt()));

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Failed to insert into bids");
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error saving bid transaction: " + e.getMessage());
        }
    }

    //Get
    public List<BidTransaction> findByAuction(String auctionId){

            String sql="select id,bidder_id,amount,bid_time "+
                    "from bids "+
                    "where auction_id= ? "+
                    "order by bid_time asc"; //xep tu cu den moi
            List <BidTransaction> result=new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
                stmt.setString(1,auctionId);
                ResultSet rs=stmt.executeQuery();
                UserDao userDao = new UserDao();

                while(rs.next()){
                    String bidderId=rs.getString("bidder_id");
                    Bidder bidder = (Bidder)userDao.findUserId(bidderId);
                    if (bidder == null) {
                        bidder = new Bidder("Unknown", "", ""); // fallback
                        bidder.setId(bidderId);
                    }
                    long amount=rs.getLong("amount");
                    BidTransaction bt=new BidTransaction(bidder,amount,auctionId);
                    bt.setId(rs.getString("id"));

                    LocalDateTime createdAt = TimeUtil.fromIso(rs.getString("bid_time"));
                    if (createdAt != null) bt.setCreatedAt(createdAt);
                    result.add(bt);
                }
            }
            catch (SQLException e){
                throw new DatabaseException("Error fetching bid transactions for auction " + auctionId);
            }
            return result;
    }

    //Gioi han số lần bid - code trước <có thể dùng>
    public int countBidsbyBidder(String auctionId,String bidderId){
        String sql="select count(*) from bids "+
                "where auction_id=? and bidder_id=? ";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,auctionId);
            stmt.setString(2,bidderId);
            ResultSet rs=stmt.executeQuery();

            if(rs.next()){
                return rs.getInt(1);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Error counting bids for bidder " + bidderId + " in auction " + auctionId + ": " + e.getMessage());
        }
        return 0;
    }

    public List<BidDetails> findByBidderId(String bidderId){
        String sql =
                "SELECT " +
                        "    bt.id, " +
                        "    bt.auction_id, " +
                        "    bt.amount, " +
                        "    bt.bid_time, " +
                        "    a.status        AS auction_status, " +
                        "    a.winner_id, " +
                        "    a.current_price, "+
                        "    i.item_name " +
                        "FROM bids bt " +
                        "JOIN auctions a ON bt.auction_id = a.id " +
                        "JOIN items i    ON a.item_id = i.id " +
                        "WHERE bt.bidder_id = ? " +
                        "ORDER BY bt.bid_time DESC";

        List<BidDetails> result = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bidderId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BidDetails bd = new BidDetails(
                        rs.getString("auction_id"),
                        rs.getString("item_name"),
                        rs.getLong("amount"),
                        TimeUtil.fromIso(rs.getString("bid_time")),
                        AuctionStatus.valueOf(rs.getString("auction_status")),
                        bidderId.equals(rs.getString("winner_id"))
                                && rs.getLong("amount") == rs.getLong("current_price")
                );
                result.add(bd);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error fetching bid details for bidder " + bidderId + ": " + e.getMessage());
        }
        return result;
    }

    //Việc load toàn bộ lịch sử là quá lâu => load n điểm gần nhất
    public List<BidTransaction> findRecentByAuction(String auctionId, int limit) {
        String sql = "select id, bidder_id, amount, bid_time " +
                "from bids " +
                "where auction_id = ? " +
                "order by bid_time desc " +
                "limit ?";
        List<BidTransaction> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            UserDao userDao = new UserDao();
            while (rs.next()) {
                String bidderId = rs.getString("bidder_id");
                Bidder bidder = (Bidder) userDao.findUserId(bidderId);
                if (bidder == null) {
                    bidder = new Bidder("Unknown", "", ""); // fallback
                    bidder.setId(bidderId);
                }

                long amount = rs.getLong("amount");
                BidTransaction bt = new BidTransaction(bidder, amount, auctionId);
                bt.setId(rs.getString("id"));

                LocalDateTime createdAt = TimeUtil.fromIso(rs.getString("bid_time"));
                if (createdAt != null) bt.setCreatedAt(createdAt);
                result.add(bt);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching recent bid transactions for auction " + auctionId);
        }
        // DESC => đảo lại thành ASC (cũ thành mới) trước khi trả về
        java.util.Collections.reverse(result);
        return result;
    }

    public List<ActiveBidSummary> findActiveBidSummaries(String bidderId) {
        String sql =
                "SELECT " +
                "    bt.auction_id, " +
                "    i.item_name, " +
                "    bt.amount           AS my_last_bid, " +
                "    a.current_price, " +
                "    a.end_time, " +
                "    (a.winner_id = ?) AS is_winning " +
                "FROM ( " +
                "    SELECT auction_id, MAX(amount) AS amount " +
                "    FROM bids " +
                "    WHERE bidder_id = ? " +
                "    GROUP BY auction_id " +
                ") bt " +
                "JOIN auctions a ON bt.auction_id = a.id " +
                "JOIN items i    ON a.item_id = i.id " +
                "WHERE a.status = 'RUNNING' " +
                "  AND a.is_deleted = 0";

        List<ActiveBidSummary> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bidderId);
            stmt.setString(2, bidderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDateTime endTime = null;
                String endStr = rs.getString("end_time");
                if (endStr != null) endTime = TimeUtil.fromIso(endStr);

                result.add(new ActiveBidSummary(
                        rs.getString("auction_id"),
                        rs.getString("item_name"),
                        rs.getLong("my_last_bid"),
                        rs.getLong("current_price"),
                        rs.getBoolean("is_winning"),
                        endTime
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching active bid summaries for bidder " + bidderId + ": " + e.getMessage());
        }
        return result;
    }

    /// DELETE
    public int deleteByAuctionId(String auctionId){
        String sql = "DELETE FROM bids WHERE auction_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            return stmt.executeUpdate(); // trả số rows bị xóa, caller tự log nếu cần
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting bid transactions for auction " + auctionId + ": " + e.getMessage());
        }
    }

    public boolean deleteById(String bidId) {
        String sql = "DELETE FROM bids WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bidId);
            return stmt.executeUpdate() > 0;
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting bid transaction with id " + bidId + ": " + e.getMessage());
        }
    }
}

