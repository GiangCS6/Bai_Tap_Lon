package bai_tap_lon.server.dao;

import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.model.entity.Auction;
import bai_tap_lon.common.model.entity.AuctionStatus;
import bai_tap_lon.common.model.item.Item;
import bai_tap_lon.common.model.item.ItemFactory;
import bai_tap_lon.common.model.user.Admin;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.model.user.Seller;
import bai_tap_lon.server.dto.WonItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;




import bai_tap_lon.common.network.TimeUtil;

import java.sql.*;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDao {
    private final Connection con;

    private static final Logger logger = Logger.getLogger(AuctionDao.class.getName());

    /*public AuctionDao(){
        Connection fake=null;
        try{
            fake=DatabaseConnection.getInstance().getConnection();
        }
        catch(SQLException e){
            logger.log(Level.SEVERE,"Error connecting to the database in AuctionDao:",e);
        }
        this.con = fake;
    }*/
    public AuctionDao() {
        this.con = null; // no longer store
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    //save
    public void save(Auction auction) {
        String sql="insert into auctions "+
                "(id,item_id,status,current_price,start_time,end_time,winner_id,is_deleted) "+
                "values (?,?,?,?,?,?,?,?)";

        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,auction.getId());
            stmt.setString(2,auction.getItem().getId());
            stmt.setString(3,auction.getStatus().toString());
            stmt.setLong(4,auction.getCurrentPrice());
            stmt.setString(5, TimeUtil.toIso(auction.getStartTime()));
            stmt.setString(6, TimeUtil.toIso(auction.getEndTime()));
            stmt.setNull(7,java.sql.Types.VARCHAR);
            stmt.setBoolean(8,false);

            int rows=stmt.executeUpdate();
            if(rows==0){throw new SQLException("Failed to insert into auction");}
        }
        catch (SQLException e){
            throw new DatabaseException("Error saving auction: "+e.getMessage());
        }
    }

    //update winner
    public void updateBid(String auctionId,String winnerId,long newPrice){
        String sql="update auctions "+
                "set winner_id=? , current_price=? "+
                "where id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,winnerId);
            stmt.setLong(2,newPrice);
            stmt.setString(3,auctionId);

            int rows=stmt.executeUpdate();
            if(rows==0){ throw new SQLException("Failed to update into auction");}
        }

        catch (SQLException e){
            throw new DatabaseException("Error updating auction bid: "+e.getMessage());
        }
    }

    //update auction status
    public void updateStatus(String auctionId, AuctionStatus status){
        String sql="update auctions "+
                "set status=? "+
                "where id=? ";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,status.toString());
            stmt.setString(2,auctionId);

            int rows=stmt.executeUpdate();
            if(rows==0){throw new SQLException ("Failed to update into auction");}
        }
        catch (SQLException e){
            throw new DatabaseException("Error updating auction status: "+e.getMessage());
        }
    }

    //update end time
    public void updateEndTime(String auctionId, LocalDateTime newEndTime){
        String sql="update auctions set end_time=? where id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, TimeUtil.toIso(newEndTime));
            stmt.setString(2,auctionId);

            int rows=stmt.executeUpdate();
            if (rows==0){throw new SQLException("Failed to update into auction");}
        }
        catch (SQLException e){
            throw new DatabaseException("Error updating auction end time: "+e.getMessage());
        }
    }

    //list running auction
    public List<Auction> findAllRunning() {
            return findByStatus("RUNNING");
    }

    //list OPEN auctions — dùng khi server boot để recover
    public List<Auction> findAllOpen(){
            return findByStatus("OPEN");
    }

    //list OPEN + RUNNING — dùng khi server recover sau crash
    public List<Auction> findAllOpenAndRunning(){
            String sql = """
                    select
                    a.id as auction_id,
                    a.status,
                    a.current_price,
                    a.start_time,
                    a.end_time,
                    a.winner_id,
                    
                    i.id as item_id,
                    i.item_name,
                    i.description,
                    i.starting_price,
                    i.category,
                    i.image_Url,
                    i.attributes,

                    s.id as seller_id,
                    s.username as seller_username,

                    w.username as winner_username

                    from auctions a
                    join items i on a.item_id = i.id
                    join users s on i.seller_id = s.id
                    left join users w on a.winner_id = w.id
                    where a.status IN ('OPEN','RUNNING') and a.is_deleted = 0
                    """;
            return mapResultSet(sql, null);
    }


    private List<Auction> findByStatus(String status){
            String sql = """
                    select
                    a.id as auction_id,
                    a.status,
                    a.current_price,
                    a.start_time,
                    a.end_time,
                    a.winner_id,
                    
                    i.id as item_id,
                    i.item_name,
                    i.description,
                    i.starting_price,
                    i.category,
                    i.image_Url,
                    i.attributes,

                    s.id as seller_id,
                    s.username as seller_username,

                    w.username as winner_username

                    from auctions a
                    join items i on a.item_id = i.id
                    join users s on i.seller_id = s.id
                    left join users w on a.winner_id = w.id
                    where a.status = ? and a.is_deleted = 0
                    """;
            return mapResultSet(sql, status);
    }

    // Helper chung — map ResultSet thành List<Auction>
    private List<Auction> mapResultSet(String sql, String statusParam) {
        List<Auction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            if (statusParam != null) stmt.setString(1, statusParam);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Error mapping ResultSet to auctions: "+e.getMessage());
        }
        return list;
    }

    // Map 1 row ResultSet → Auction object
    private Auction mapRow(ResultSet rs){
        try {
            String id = rs.getString("auction_id");
            String status = rs.getString("status");
            long currentPrice = rs.getLong("current_price");
            String startStr = rs.getString("start_time");
            String endStr = rs.getString("end_time");
            String winnerId = rs.getString("winner_id");

            Seller seller = new Seller(rs.getString("seller_username"), null, null);
            seller.setId(rs.getString("seller_id"));

            Bidder bidder = null;
            if (winnerId != null) {
                bidder = new Bidder(rs.getString("winner_username"), null, null);
                bidder.setId(winnerId);
            }

            //item
            String category = rs.getString("category");
            String name = rs.getString("item_name");
            String description = rs.getString("description");
            long startingPrice = rs.getLong("starting_price");
            String imageUrl = rs.getString("image_Url");

            JsonObject attrs = parseJsonSafe(rs.getString("attributes"));
            Item item = ItemFactory.createItem(category, name, description, startingPrice, seller,imageUrl, attrs);
            item.setImageUrl(imageUrl);

            // BidExecutor sẽ được inject sau khi AuctionManager nhận Auction này
            Auction auction = new Auction(item, TimeUtil.fromIso(startStr), TimeUtil.fromIso(endStr), null);
            auction.setId(id);
            auction.setStatus(AuctionStatus.valueOf(status));
            auction.setCurrentPrice(currentPrice);
            auction.setCurrentWinner(bidder);
            return auction;
        }
        catch (SQLException e){
            throw new DatabaseException("Error mapping ResultSet row to auction: "+e.getMessage());
        }
    }

    //check the existing of auction
    public boolean check(String auctionId) {
        String sql="select count(*) from auctions where id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,auctionId);

            ResultSet rs=stmt.executeQuery();
            if(rs.next()){return rs.getInt(1)>0;} //luon chay
        }
        catch (SQLException e){
            throw new DatabaseException("Error checking auction existence: "+e.getMessage());
        }
        return false; //tranh loi compile
    }

    //find all auction
    public List<Auction> findAllAuction(){
        String sql = """
            select
            a.id as auction_id,
            a.status,
            a.current_price,
            a.start_time,
            a.end_time,
            a.winner_id,

            i.id as item_id,
            i.item_name,
            i.description,
            i.starting_price,
            i.category,
            i.image_Url,
            i.attributes,

            s.id as seller_id,
            s.username as seller_username,

            w.username as winner_username

            from auctions a
            join items i on a.item_id = i.id
            join users s on i.seller_id = s.id
            left join users w on a.winner_id = w.id
            where a.is_deleted = 0
            order by a.start_time desc
            """;

        List<Auction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Error finding all auctions: "+e.getMessage());
        }
        return list;
    }

    //find by id
    public Auction findById(String auctionId) {
        String sql = """
                select
                a.id as auction_id,
                a.status,
                a.current_price,
                a.start_time,
                a.end_time,
                a.winner_id,

                i.id as item_id,
                i.item_name,
                i.description,
                i.starting_price,
                i.category,
                i.image_Url,
                i.attributes,

                s.id as seller_id,
                s.username as seller_username,

                w.username as winner_username

                from auctions a
                join items i on a.item_id = i.id
                join users s on i.seller_id = s.id
                left join users w on a.winner_id = w.id
                where a.id = ? and a.is_deleted = 0
                """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        catch (SQLException e){
            throw new DatabaseException("Error finding auction by id: "+e.getMessage());
        }
        return null;
    }

    public List<WonItem> findWonItems(String bidderId) {
        String sql =
                "SELECT " +
                "    a.id AS auction_id, " +
                "    i.item_name, " +
                "    a.current_price AS final_price, " +
                "    a.status AS auction_status, " +
                "    a.end_time " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "WHERE a.winner_id = ? " +
                "  AND a.status IN ('FINISHED', 'PAID') " +
                "  AND a.is_deleted = 0 " +
                "ORDER BY a.end_time DESC";

        List<WonItem> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, bidderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDateTime endTime = null;
                String endStr = rs.getString("end_time");
                if (endStr != null) endTime = TimeUtil.fromIso(endStr);

                result.add(new WonItem(
                        rs.getString("auction_id"),
                        rs.getString("item_name"),
                        rs.getLong("final_price"),
                        rs.getString("auction_status"),
                        endTime
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching won items for bidder " + bidderId + ": " + e.getMessage());
        }
        return result;
    }

    /// DELETE
    public boolean deleteById(String auctionId){
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, auctionId);
            return stmt.executeUpdate() > 0;
        }
        catch (SQLException e){
            throw new DatabaseException("Error deleting auction: "+e.getMessage());
        }
    }

    private JsonObject parseJsonSafe(String json) {
        if (json == null || json.trim().isEmpty()) return new JsonObject();
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[AuctionDao] Malformed attributes JSON: " + json, e);
            return new JsonObject();
        }
    }

}