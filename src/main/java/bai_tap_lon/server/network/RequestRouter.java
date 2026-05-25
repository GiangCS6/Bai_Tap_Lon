package bai_tap_lon.server.network;

import bai_tap_lon.common.exception.BusinessException;
import bai_tap_lon.common.exception.DatabaseException;
import bai_tap_lon.common.exception.UnauthorizedException;
import bai_tap_lon.common.model.entity.*;
import bai_tap_lon.common.model.entity.AutoBidSetting;
import bai_tap_lon.common.model.item.Item;
import bai_tap_lon.common.model.item.ItemFactory;
import bai_tap_lon.common.model.user.*;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.Response;
import bai_tap_lon.common.network.TimeUtil;
import bai_tap_lon.server.config.ImageConfig;
import bai_tap_lon.server.dao.*;
import bai_tap_lon.server.dto.ActiveBidSummary;
import bai_tap_lon.server.dto.BidDetails;
import bai_tap_lon.server.dto.ItemDetails;
import bai_tap_lon.server.dto.ItemInfoDetails;
import bai_tap_lon.server.dto.WonItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.*;

import static bai_tap_lon.server.network.ResponseFactory.error;
import static bai_tap_lon.server.network.ResponseFactory.ok;

/**
 * Routing tất cả action từ client → xử lý → trả Response.
 * Mỗi action trong JSON Protocol Spec có 1 case tương ứng ở đây.
 */

public class RequestRouter {
    private static final int BID_HISTORY_LIMIT = 20;
    private final WatchListDao watchListDao;
    private final UserDao userDao;
    private final AuctionDao auctionDao;
    private final BidDao bidDao;
    private final AutoBidDao autoBidDao;
    private final ItemDAO itemDAO;
    public final AuctionManager auctionManager;

    /// Constructor
    public RequestRouter(
            UserDao userDao,
            AuctionDao auctionDao,
            BidDao bidDao,
            ItemDAO itemDAO,
            AuctionManager auctionManager,
            WatchListDao watchListDao) {
        this.userDao = userDao;
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.itemDAO = itemDAO;
        this.auctionManager = auctionManager;
        this.watchListDao = watchListDao;
        this.autoBidDao = new AutoBidDao();

    }

    /// CHECK ACTION AND ROUTE
    public Response route(Request request, ClientHandler clientHandler) {
        String action = request.getAction().trim().toUpperCase(Locale.ROOT);//ĐA ngôn ngữ, tránh lỗi do viết hoa thường
        JsonObject payload = (request.getPayload() == null) ? new JsonObject() : request.getPayload();

        try {
            /// Check login or register first
            if (action.equals("LOGIN")) {
                return handleLogin(action, payload, clientHandler);
            } else if (action.equals("REGISTER")) {
                return handleRegister(action, payload);
            }

            /// Check if user is authenticated for other actions
            User current = clientHandler.getUser();
            if (current == null) {
                return error(action, "UNAUTHORIZED", "Ban can dang nhap truoc");
            }

            /// ROUTE OTHER ACTIONS
            switch (action) {
                case "BAN_USER":
                    return handleBanUser(action, payload, current);
                case "UNBAN_USER":
                    return handleUnbanUser(action, payload, current);
                case "GET_ALL_AUCTIONS":
                    return handleGetAllAuctions(action, current);
                case "CANCEL_AUCTION":
                    return handleCancelAuction(action, payload, current);

                case "GET_OPEN_AND_RUNNING_AUCTIONS":
                    return handleGetOpenAndRunningAuctions(action, payload);
                case "GET_ACTIVE_AUCTIONS":
                    return handleGetActiveAuctions(action,payload);
                case "GET_AUCTION_DETAIL":
                    return handleGetAuctionDetail(action, payload,current);
                case "GET_ITEM_DETAILS":
                    return handleGetItemDetails(action, payload);
                case "POST_ITEM":
                    return handlePostItem(action, payload, current);
                case "DELETE_ITEM":
                    return handleDeleteItem(action, payload, current);
                case "GET_MY_ITEMS":
                    return handleGetMyItems(action, current);

                case "PLACE_BID":
                    return handlePlaceBid(action, payload, current);
                case "SET_AUTO_BID":    return handleSetAutoBid(action, payload, current);
                case "CANCEL_AUTO_BID": return handleCancelAutoBid(action, payload, current);
                case "GET_AUCTION_BID_HISTORY": return handleGetAuctionBidHistory(action, payload);
                case "GET_MY_BIDS":     return handleGetMyBids(action, current);
                case "GET_ACTIVE_BID_SUMMARY": return handleGetActiveBidSummary(action, current);
                case "GET_WON_ITEMS":           return handleGetWonItems(action, current);

                case "SAVE_WATCH_AUCTION": return handleSaveWatchAuction(action, payload, current, clientHandler);
                case "DELETE_WATCH_AUCTION": return handleDeleteWatchAuction(action, payload, current, clientHandler);
                case "WATCH_AUCTION":   return handleWatchAuction(action, payload, current, clientHandler);
                case "UNWATCH_AUCTION": return handleUnwatchAuction(action, payload, current, clientHandler);
                case "GET_WATCHLIST":   return handleGetWatchlist(action, current);

                case "DEPOSIT":     return handleDeposit(action, payload, current);
                case "WITHDRAW":    return handleWithdraw(action, payload, current);
                case "GET_BALANCE": return handleGetBalance(action, current);

                default:
                    return error(action, "VALIDATION_ERROR", "Action khong ho tro: " + action);
            }
        } catch (UnauthorizedException e) {
            return error(action, "UNAUTHORIZED", e.getMessage());
        } catch (BusinessException e) {
            return error(action, e.getCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(action, "VALIDATION_ERROR", e.getMessage());
        } catch (DatabaseException e) {
            return error(action, "DB_ERROR", "Loi co so du lieu: " + e.getMessage());
        } catch (Exception e) {
            return error(action, "SERVER_ERROR", "Loi may chu noi bo" + e.getMessage());
        }
    }

    // HANDLE REGISTER ACTIONN
    public Response handleRegister (String action, JsonObject payload){
        String username = reqString(payload, "username");
        String password = reqString(payload, "password");
        String email = reqString(payload, "email");
        String role = reqString(payload, "role");

        // check username
        if (userDao.checkUserName(username))
            throw new BusinessException("USERNAME_TAKEN", "Username da duoc su dung");

        //check role
        User u;
        switch (role) {
            case "BIDDER":
                u = new Bidder(username, password, email);
                break;
            case "SELLER":
                u = new Seller(username, password, email);
                break;
            case "ADMIN":
                u = new Admin(username, password, email);
                break;
            default:
                throw new IllegalArgumentException("role khong hop le: " + role);
        }
        userDao.save(u);

        // return Response
        JsonObject data = new JsonObject();
        data.addProperty("userId", u.getId());
        data.addProperty("message", "Registration Success");
        return ok(action, data);
    }

    /*public Response handleLogin(String action, JsonObject payload, ClientHandler clientHandler){
        String email = reqString(payload, "email");
        String password = reqString(payload, "password");

        User user = userDao.findByEmail(email);
        if (user == null) throw new BusinessException("USER_NOT_FOUND", "Khong tim thay tai khoan");
        if (!user.getPassword().equals(password)) throw new BusinessException("WRONG_PASSWORD", "Sai mat khau");
        if (!user.isActive()) throw new BusinessException("ACCOUNT_BANNED", "Tai khoan da bi khoa");*/
    public Response handleLogin(String action, JsonObject payload, ClientHandler clientHandler){
        String username = reqString(payload, "username");   // Changed from email
        String password = reqString(payload, "password");

        User user = userDao.findUserName(username);         // Changed to find by username
        if (user == null) throw new BusinessException("USER_NOT_FOUND", "Username không tồn tại");

        if (!user.getPassword().equals(password))
            throw new BusinessException("WRONG_PASSWORD", "Sai mật khẩu");

        if (!user.isActive())
            throw new BusinessException("ACCOUNT_BANNED", "Tài khoản đã bị khóa");

        //Save current User to clientHandler
        clientHandler.setUser(user);

        //Load watchList và register observer
        List<String> auctionId = watchListDao.findWatchedAuctionIds(user.getId());
        for (String id: auctionId){
             Auction auc = auctionManager.getAuction(id);
             if (auc != null && (auc.getStatus() == AuctionStatus.RUNNING || auc.getStatus() == AuctionStatus.OPEN)) {
                 auc.addObserver(clientHandler);
             }
             else {
                 watchListDao.unwatch(user.getId(), id);
             }
        }

        JsonObject ju = new JsonObject();
        ju.addProperty("id", user.getId());
        ju.addProperty("username", user.getUsername());
        ju.addProperty("email", user.getEmail());
        ju.addProperty("role", user.getRole());
        ju.addProperty("isActive", user.isActive());
        ju.addProperty("createdAt", TimeUtil.toIso(user.getCreatedAt())); // FIX (1)

        JsonObject data = new JsonObject();
        if (user instanceof HasBalance hasBalance) {
            data.addProperty("balance", hasBalance.getBalance());
        }
        data.add("user", ju);
        return ok(action, data);

    }

    /// ____ADMIN_____
    public Response handleBanUser(String action, JsonObject payload, User current){
        requireAdmin(current);

        String targetUsername = reqString(payload, "targetUsername");
        String reason = reqString(payload, "reason");

        User target = userDao.findUserName(targetUsername);
        if (target == null) throw new BusinessException("USER_NOT_FOUND", "Khong tim thay user");
        if (target instanceof Admin) throw new BusinessException("UNAUTHORIZED", "Khong the ban Admin khac");

        userDao.updateActive(target.getId(), false);

        JsonObject data = new JsonObject();
        data.addProperty("targetUsername", targetUsername);
        data.addProperty("isActive", false);
        data.addProperty("reason", reason);
        data.addProperty("bannedAt", TimeUtil.toIso(TimeUtil.now()));
        return ok(action, data);
    }

    private Response handleUnbanUser(String action, JsonObject p, User current){
        requireAdmin(current);

        String targetId = reqString(p, "targetUserId");

        User target = userDao.findUserId(targetId);
        if (target == null) throw new BusinessException("USER_NOT_FOUND", "Khong tim thay user");

        userDao.updateActive(targetId, true);

        JsonObject data = new JsonObject();
        data.addProperty("targetUserId", targetId);
        data.addProperty("isActive", true);
        data.addProperty("unbannedAt", TimeUtil.toIso(TimeUtil.now()));
        return ok(action, data);
    }

    private Response handleGetAllAuctions(String action, User current){
        requireAdmin(current);
        List<Auction> auctions = auctionDao.findAllAuction();
        JsonArray arr = new JsonArray();
        for (Auction a : auctions) arr.add(toAuctionSummary(a));

        JsonObject data = new JsonObject();
        data.add("auctions", arr);
        return ok(action, data);
    }

    private Response handleCancelAuction(String action, JsonObject p, User current){
        requireAdmin(current);

        String auctionId = reqString(p, "auctionId");
        String reason = reqString(p, "reason");
        Auction auction = auctionManager.getAuction(auctionId);

        if (auction == null) throw new BusinessException("AUCTION_NOT_FOUND", "Auction not found");

        // cancel only if running or not started
        AuctionStatus st = auction.getStatus();
        if (st == AuctionStatus.FINISHED || st == AuctionStatus.CANCELED || st == AuctionStatus.PAID) {
            throw new BusinessException("VALIDATION_ERROR", "Cannot canceled when auction is: " + st);
        }
        //update DB
        auctionDao.updateStatus(auctionId, AuctionStatus.CANCELED);
        bidDao.deleteByAuctionId(auctionId);

        // PUSH TO OBSERVER and RAM
        auctionManager.cancelAuction(auctionId,reason);

        //Response
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("status", "CANCELED");
        data.addProperty("canceledBy", current.getUsername());
        data.addProperty("reason", reason);
        data.addProperty("canceledAt", TimeUtil.toIso(TimeUtil.now()));
        return ok(action, data);
    }

    ///  AUCTION / ITEM
    // ===================================================================
    private Response handleGetActiveAuctions(String action, JsonObject p) {
       // String category = reqString(p, "category");
        List<Auction> auctions = auctionDao.findAllRunning();
        JsonArray arr = new JsonArray();
        for (Auction auc: auctions){
            arr.add(toAuctionSummary(auc));
        }
        JsonObject data = new JsonObject();
        data.add("auctions", arr);

        return ok(action,data);
    }

    private Response handleGetOpenAndRunningAuctions(String action, JsonObject p){
        List<Auction> auctions = auctionDao.findAllOpenAndRunning();
        JsonArray arr = new JsonArray();
        for (Auction auc: auctions){
            arr.add(toAuctionSummary(auc));
        }
        JsonObject data = new JsonObject();
        data.add("auctions", arr);

        return ok(action,data);
    }
    private Response handleGetAuctionDetail(String action, JsonObject p,User current){
        String auctionId = reqString(p, "auctionId");

        // Đọc từ RAM để có currentPrice, currentWinner mới nhất
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) throw new BusinessException("AUCTION_NOT_FOUND", "Khong tim thay auction");

        List<BidTransaction> bids = bidDao.findRecentByAuction (auctionId,BID_HISTORY_LIMIT);
        JsonArray bidHitory = new JsonArray();
        for (BidTransaction b : bids) {
            bidHitory.add(toBidJson(b));
        }

        JsonObject data = new JsonObject();
        data.add("auction", toAuctionDetail(auction));
        data.add("bidHistory", bidHitory);

        // Trả về autobid setting của bidding hiện tại ( nếu có )
        if (current instanceof Bidder) {
            AutoBidSetting autobid = autoBidDao.findByBidderAndAuction(current.getId(), auctionId);
            if (autobid != null) {
                JsonObject ab = new JsonObject();
                ab.addProperty("maxBid",    autobid.getMaxBid());
                ab.addProperty("increment", autobid.getIncrement());
                data.add("myAutoBid", ab);
            }
        }

        return ok(action, data);
    }

    private Response handleGetItemDetails(String action, JsonObject p) {
        String auctionId = reqString(p, "auctionId");

        ItemInfoDetails details = itemDAO.getItemDetailsByAuctionId(auctionId);
        if (details == null) {
            throw new BusinessException("AUCTION_NOT_FOUND", "Khong tim thay auction hoac item");
        }

        // Build full image URL
        JsonObject data = details.toJson();
        data.addProperty("imageUrl", ImageConfig.buildPublicImageUrl(details.getImageUrl()));
        return ok(action, data);
    }

    private Response handlePostItem(String action, JsonObject p, User current){
        requireSeller(current);

        String name = reqString(p, "name");

        String description = reqString(p, "description");
        long startingPrice = reqLong(p, "startingPrice"); // FIX (2): long
        String category = reqString(p, "category");
        int durationMin = reqInt(p, "auctionDurationMinutes");
        String startTimeStr = reqString(p,"startTime");
        String imageUrl = reqString(p,"imageUrl");
        LocalDateTime startTime;

        try{
            startTime = TimeUtil.fromIso(startTimeStr);
        }catch (Exception e){
            throw new BusinessException("INVALID_TIME_FORMAT", "Invalid time format");
        }
        // Validate: startTime phải >= now (cho phép cách hiện tại 1 phút để bù sai số)
        if (startTime.isBefore(TimeUtil.now().minusMinutes(1))) {
            throw new BusinessException("VALIDATION_ERROR",
                    "startTime phai la thoi diem hien tai hoac tuong lai");
        }

        if (durationMin < 5 || durationMin > 10080) {
            throw new BusinessException("VALIDATION_ERROR", "auctionDurationMinutes phai tu 5 den 10080");
        }
        if (startingPrice <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "startingPrice phai > 0");
        }

        ///xem lại
        Seller seller = (Seller) current;
        //refactor 18 dòng switch case thành cái này
        JsonObject attrs = p.has("attributes") && p.get("attributes").isJsonObject()
                ? p.getAsJsonObject("attributes")
                : new JsonObject();

        if (!ItemFactory.isValidCategory(category)) {
            throw new BusinessException("VALIDATION_ERROR", "category khong hop le: " + category);
        }

        Item item = ItemFactory.createItem(category, name, description, startingPrice, seller, imageUrl, attrs);

        boolean saved = itemDAO.save(item);
        if (!saved) throw new BusinessException("DB_ERROR", "Khong the luu item vao database");

        // FIX (7): OPEN — khong startAuction()
//        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationMin);
        Auction auction = new Auction(item, startTime, endTime,null);

        auction.setBidExecutor(createBidExecutor(auction));

        auctionDao.save(auction);
        // onAuctionPosted: addAuction (timer + map) + bắn lifecycle event
        // → Server.broadcastToAll(AUCTION_CREATED) cho mọi client realtime
        auctionManager.onAuctionPosted(auction);

        JsonObject data = new JsonObject();
        data.addProperty("itemId", item.getId());
        data.addProperty("auctionId", auction.getId());
        data.addProperty("message", "Tao san pham va phien dau gia thanh cong");
        return ok(action, data);
    }

    private Response handleDeleteItem(String action, JsonObject p, User current){
        requireSeller(current);
        String itemId = reqString(p, "itemId");
        Auction auction = auctionManager.getAuction(itemId);

        if (auction == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "Item not found");
        }

        if (auction.isActive()) {
            throw new BusinessException("VALIDATION_ERROR", "Cannot delete item while the auction is running");
        }
        //Delete DB
        auctionDao.deleteById(auction.getId());
        itemDAO.deleteById(itemId);
        auctionManager.delete(itemId);

        JsonObject data = new JsonObject();
        data.addProperty("itemId",  itemId);
        data.addProperty("message", "Item deleted successfully");
        return ok(action, data);
    }

    private Response handleGetMyItems(String action, User current){
        requireSeller(current);

        List<ItemDetails> items=itemDAO.findBySellerId(current.getId());//sửa chỗ này
        JsonArray arr = new JsonArray();

        for (ItemDetails item : items) {
            arr.add(item.toJson());
        }

        JsonObject data = new JsonObject();
        data.add("items", arr);
        return ok(action, data);
    }

    // ===================================================================
    ///  BID

    private BidExecutor createBidExecutor(Auction auction) {
        return (bidder, amount) -> executeBid(bidder, amount, auction);
    }

    public AuctionManager.BidExecutorFactory getBidExecutorFactory() {
        return this::createBidExecutor;
    }

    private BidTransaction executeBid(Bidder bidder, long amount, Auction auc){

        Bidder oldWinner = auc.getCurrentWinner();
        long   oldPrice  = auc.getCurrentPrice();

        // 1. Trừ tiền bidder mới — atomic WHERE balance >= amount
        boolean deducted = userDao.decrementBalance(bidder.getId(), amount);
        if (!deducted) {
            throw new BusinessException("INSUFFICIENT_BALANCE",
                    "So du khong du de dat gia: " + amount);
        }

        // 2. Hoàn tiền oldWinner nếu có và khác bidder hiện tại
        if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
            try {
                userDao.incrementBalance(oldWinner.getId(), oldPrice);
                oldWinner.setBalance(oldWinner.getBalance() + oldPrice);
            } catch (DatabaseException e) {
                // Hoàn tiền thất bại → rollback tiền bidder mới
                userDao.incrementBalance(bidder.getId(), amount);
                throw new DatabaseException("Error occurred while refunding old winner: " + e.getMessage());
            }
        }

        // 3. Update RAM: validate + update currentPrice/currentWinner + anti-sniping + notify
        BidTransaction tx;
        try {
            tx = auc.placeBid(bidder, amount);
        } catch (BusinessException e) {
            // Validate thất bại → rollback tài chính
            userDao.incrementBalance(bidder.getId(), amount);
            if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
                userDao.decrementBalance(oldWinner.getId(), oldPrice);
                oldWinner.setBalance(oldWinner.getBalance() - oldPrice);
            }
            throw e;
        }

        // 4. Lưu DB
        try {
            bidDao.save(tx);
            auctionDao.updateBid(auc.getId(), bidder.getId(), amount);
        } catch (DatabaseException e) {
            // DB lưu thất bại → rollback RAM và tài chính
            auc.setCurrentPrice(oldPrice);
            auc.setCurrentWinner(oldWinner);
            userDao.incrementBalance(bidder.getId(), amount);
            if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
                userDao.decrementBalance(oldWinner.getId(), oldPrice);
                oldWinner.setBalance(oldWinner.getBalance() - oldPrice);
            }
            throw new DatabaseException("Error occurred while saving bid transaction: " + e.getMessage());
        }

        // Sync RAM balance bidder mới
        bidder.setBalance(bidder.getBalance() - amount);

        return tx;
    }

    private Response handlePlaceBid(String action, JsonObject p, User current){
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");
        long amount = reqLong(p, "amount");

        Auction auc = auctionManager.getAuction(auctionId);
        if (auc == null) {
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        }

        BidTransaction tx;
        boolean timeExtended;
        LocalDateTime newEndTime;

        synchronized (auc) {


            Bidder oldWinner = auc.getCurrentWinner();
            long oldPrice = auc.getCurrentPrice();
            LocalDateTime oldEndTime = auc.getEndTime();


            boolean deducted = userDao.decrementBalance(current.getId(), amount);
            if (!deducted) {
                throw new BusinessException("INSUFFICIENT_BALANCE", "So du khong du: " + amount);
            }

            // hoàn tiền
            if (oldWinner != null && !oldWinner.getId().equals(current.getId())) {
                try {
                    userDao.incrementBalance(oldWinner.getId(), oldPrice);
                    oldWinner.setBalance(oldWinner.getBalance() + oldPrice);
                } catch (DatabaseException e) {
                    // rollback
                    userDao.incrementBalance(current.getId(), amount);
                    throw new DatabaseException("Error occurred while refunding: " + e.getMessage());
                }
            }

            //update RAM
            try {
                tx = auc.placeBid((Bidder) current, amount);
            } catch (BusinessException e) {
                // rollback tiền
                userDao.incrementBalance(current.getId(), amount);

                if (oldWinner != null && !oldWinner.getId().equals(current.getId())) {
                    userDao.decrementBalance(oldWinner.getId(), oldPrice);
                    oldWinner.setBalance(oldWinner.getBalance() - oldPrice);
                }
                throw e;
            }

            //  save DB
            bidDao.save(tx);
            auctionDao.updateBid(auctionId, current.getId(), amount);

            //  sync RAM bidder
            ((Bidder) current).setBalance(((Bidder) current).getBalance() - amount);

            // check extend time
            newEndTime = auc.getEndTime();
            timeExtended = !newEndTime.equals(oldEndTime);

            if (timeExtended) {
                auctionDao.updateEndTime(auctionId, newEndTime);
            }
        }


        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("newPrice", amount);
        data.add("transaction", toBidJson(tx));
        data.addProperty("timeExtended", timeExtended);

        if (timeExtended) {
            data.addProperty("newEndTime", TimeUtil.toIso(newEndTime));
        } else {
            data.add("newEndTime", JsonNull.INSTANCE);
        }

        return ok(action, data);
    }

    private Response handleSetAutoBid(String action, JsonObject p, User current){
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");
        long   maxBid    = reqLong(p, "maxBid");
        long   increment = reqLong(p, "increment");

        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null)
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        if (auction.getStatus() != AuctionStatus.RUNNING)
            throw new BusinessException("AUCTION_NOT_RUNNING", "Auction is not running");

        // Validate maxBid >= currentPrice + increment (ít nhất phải bid được 1 lần)
        if (maxBid < auction.getCurrentPrice() + increment)
            throw new BusinessException("VALIDATION_ERROR",
                    "maxBid phai >= currentPrice + increment (" +
                            (auction.getCurrentPrice() + increment) + ")");

        Bidder bidder = (Bidder) current;
        // Lưu setting vào Bidder
        bidder.setAutoBid(maxBid, increment);
        // Register vào AutoBidManager của auction này để trigger() tìm thấy
        auction.getAutoBidManager().registerBidder(bidder);

        autoBidDao.save(bidder.getId(), auctionId, maxBid, increment);

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("maxBid",    maxBid);
        data.addProperty("increment", increment);
        data.addProperty("message",   "AutoBid da duoc kich hoat");
        return ok(action, data);
    }

    private Response handleCancelAutoBid(String action, JsonObject p, User current){
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");

        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        }
        Bidder bidder = (Bidder) current;
        // Hủy setting trong Bidder
        bidder.cancelAutoBid();
        // Unregister khỏi AutoBidManager của auction này
        auction.getAutoBidManager().unregisterBidder(current.getId());

        //huy trong ram
        autoBidDao.deactivate(bidder.getId(), auctionId);
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("message",   "AutoBid cancelled succesfully");
        return ok(action, data);
    }
    private Response handleGetAuctionBidHistory(String action, JsonObject p){
        String auctionId = reqString(p, "auctionId");

        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) throw new BusinessException("AUCTION_NOT_FOUND", "Auction not found");

        List<BidTransaction> bids = bidDao.findByAuction(auctionId);
        JsonArray arr = new JsonArray();
        for (BidTransaction bt : bids) arr.add(toBidJson(bt));

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.add("bids", arr);
        return ok(action, data);
    }

    private Response handleGetMyBids(String action, User current){
        requireBidder(current);

        List<BidDetails> myBids = bidDao.findByBidderId(current.getId());

        JsonArray arr = new JsonArray();
        for (BidDetails bd : myBids) {
            arr.add(bd.toJson());
        }

        JsonObject data = new JsonObject();
        data.add("bids", arr);
        return ok(action, data);
    }

    private Response handleGetActiveBidSummary(String action, User current) {
        requireBidder(current);

        List<ActiveBidSummary> summaries = bidDao.findActiveBidSummaries(current.getId());

        JsonArray arr = new JsonArray();
        for (ActiveBidSummary s : summaries) {
            arr.add(s.toJson());
        }

        JsonObject data = new JsonObject();
        data.add("activeBids", arr);
        return ok(action, data);
    }

    private Response handleGetWonItems(String action, User current) {
        requireBidder(current);

        List<WonItem> wonItems = auctionDao.findWonItems(current.getId());

        JsonArray arr = new JsonArray();
        for (WonItem w : wonItems) {
            arr.add(w.toJson());
        }

        JsonObject data = new JsonObject();
        data.add("wonItems", arr);
        return ok(action, data);
    }

    // ===================================================================
    ///  REALTIME

    private Response handleSaveWatchAuction(String action, JsonObject p, User current, ClientHandler ch){
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");
        Auction auction = auctionManager.getAuction(auctionId);

        if (auction == null){
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        }
        watchListDao.watch(current.getId(), auctionId);
        auction.addObserver(ch);
        ch.addWatchingAuction(auctionId);

        if (auction.getStatus() == AuctionStatus.RUNNING) {
            Bidder bidder = (Bidder) current;
            AutoBidSetting saved = autoBidDao.findByBidderAndAuction(bidder.getId(), auctionId);
            if (saved != null) {
                bidder.setAutoBid(saved.getMaxBid(), saved.getIncrement());
                auction.getAutoBidManager().registerBidder(bidder);
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("message",   "Following successfully");
        return ok(action, data);
    }

    private Response handleDeleteWatchAuction(String action, JsonObject p, User current, ClientHandler ch){
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");
        Auction auction = auctionManager.getAuction(auctionId);

        if (auction == null){
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        }
        watchListDao.unwatch(current.getId(), auctionId);
        auction.removeObserver(ch);
        ch.removeWatchingAuction(auctionId);

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("message",   "Unfollowing successfully");
        return ok(action, data);
    }

    private Response handleWatchAuction(String action, JsonObject p, User current, ClientHandler ch) {
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");
        Auction auction = auctionManager.getAuction(auctionId);

        if (auction == null){
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        }

        auction.addObserver(ch);
        ch.addWatchingAuction(auctionId);

        if (auction.getStatus() == AuctionStatus.RUNNING) {
            Bidder bidder = (Bidder) current;
            AutoBidSetting saved = autoBidDao.findByBidderAndAuction(bidder.getId(), auctionId);
            if (saved != null) {
                bidder.setAutoBid(saved.getMaxBid(), saved.getIncrement());
                auction.getAutoBidManager().registerBidder(bidder);
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("message",   "Following successfully");
        return ok(action, data);
    }

    private Response handleGetWatchlist(String action, User current) {
        requireBidder(current);
        List<String> ids = watchListDao.findWatchedAuctionIds(current.getId());
        JsonArray arr = new JsonArray();
        for (String id : ids) {
            Auction a = auctionManager.getAuction(id);
            if (a != null) arr.add(toAuctionSummary(a));
        }
        JsonObject data = new JsonObject();
        data.add("auctions", arr);
        return ok(action, data);
    }

    private Response handleUnwatchAuction(String action, JsonObject p, User current, ClientHandler ch){
        requireBidder(current);

        String auctionId = reqString(p, "auctionId");
        Auction auction = auctionManager.getAuction(auctionId);

        if (auction == null){
            throw new BusinessException("AUCTION_NOT_FOUND", "Cannot find this auction");
        }

        if (!watchListDao.isWatching(current.getId(), auctionId)) {
            auction.removeObserver(ch);
            ch.removeWatchingAuction(auctionId);
        }

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("message",   "Da ngung theo doi");
        return ok(action, data);
    }

    /// BALANCE
    // ===================================================================
    private Response handleDeposit(String action, JsonObject p, User current){
        requireBidder(current);
        long amount = reqLong(p, "amount");
        if (amount < 1000L) {
            throw new BusinessException("DEPOSIT_AMOUNT_TOO_LOW", "Cannot deposit lower than 1000");
        }

        userDao.incrementBalance(current.getId(), amount);
        long newBalance = ((HasBalance) current).getBalance() + amount;
        ((HasBalance) current).setBalance(newBalance); // RAM best effort

        JsonObject data = new JsonObject();
        data.addProperty("userId",          current.getId());
        data.addProperty("amountDeposited", amount);
        data.addProperty("newBalance",      newBalance);
        data.addProperty("timestamp",       TimeUtil.toIso(TimeUtil.now()));
        return ok(action, data);
    }

    private Response handleWithdraw(String action, JsonObject p, User current){
        requireHasBalance(current);
        long amount = reqLong(p, "amount");
        if (amount < 1000L) {
            throw new BusinessException("WITHDRAW_AMOUNT_TOO_LOW", "Cannot withdraw lower than 1000");
        }

        boolean success = userDao.decrementBalance(current.getId(), amount);
        if (!success) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "So du khong du");
        }

        userDao.decrementBalance(current.getId(), amount);
        long newBalance = ((HasBalance) current).getBalance() - amount;
        ((HasBalance) current).setBalance(newBalance); // RAM best effort

        JsonObject data = new JsonObject();
        data.addProperty("userId",          current.getId());
        data.addProperty("amountWithdrawn", amount);
        data.addProperty("newBalance",      newBalance);
        data.addProperty("timestamp",       TimeUtil.toIso(TimeUtil.now()));
        return ok(action, data);
    }

    private Response handleGetBalance(String action, User current) {
        requireHasBalance(current);
        long balance = userDao.findBalanceById(current.getId());

        ((HasBalance) current).setBalance(balance);

        JsonObject data = new JsonObject();
        data.addProperty("userId",           current.getId());
        data.addProperty("balance",          balance);
        return ok(action, data);
    }



    /// VALIDATORS
    // ===================================================================
    private static String reqString(JsonObject p, String field) {
        if (p == null || !p.has(field) || p.get(field).isJsonNull())
            throw new IllegalArgumentException("Missing required field: " + field);
        String v = p.get(field).getAsString();
        if (v.isBlank())
            throw new IllegalArgumentException("Field can't be blank: " + field);
        return v;
    }

    private static long reqLong(JsonObject p, String field) {
        if (p == null || !p.has(field) || p.get(field).isJsonNull())
            throw new IllegalArgumentException("Missing required field: " + field);
        try {
            return p.get(field).getAsLong();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Field is not in correct format: " + field);
        }
    }

    private static int reqInt(JsonObject p, String field) {
        if (p == null || !p.has(field) || p.get(field).isJsonNull())
            throw new IllegalArgumentException("Missing required field: " + field);
        try {
            return p.get(field).getAsInt();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Field is not in correct format: " + field);
        }
    }

    private static boolean reqBoolean(JsonObject p, String field) {
        if (p == null || !p.has(field) || p.get(field).isJsonNull())
            throw new IllegalArgumentException("Thieu field bat buoc: " + field);
        JsonElement element = p.get(field);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Field " + field + " phai la kieu Boolean (true/false)");
        }
        return element.getAsBoolean();
    }

    // ===================================================================

    ///  AUTHENTICATOR
    // ===================================================================
    private static void requireAdmin(User u) {
        if (!(u instanceof Admin))
            throw new UnauthorizedException("Only Admin can perform this task");
    }

    private static void requireSeller(User u) {
        if (!(u instanceof Seller))
            throw new UnauthorizedException("Only Seller can perform this task");
    }

    private static void requireBidder(User u) {
        if (!(u instanceof Bidder))
            throw new UnauthorizedException("Only Bidder can perform this task");
    }

    private static void requireHasBalance(User u){
        if (!(u instanceof HasBalance))
            throw new UnauthorizedException("Only account that has balance can perform this action");
    }

    /// _____MAPPERS____
    public static JsonObject toAuctionSummary(Auction a) {
        Item i = a.getItem();
        JsonObject o = new JsonObject();
        o.addProperty("id", a.getId());
        o.addProperty("itemName", i.getName());;
        o.addProperty("currentPrice", a.getCurrentPrice());                            // FIX (2)
        o.addProperty("status", a.getStatus().name());
        o.addProperty("startTime", a.getStartTime() != null ? TimeUtil.toIso(a.getStartTime()) : null);
        o.addProperty("endTime", a.getEndTime() != null ? TimeUtil.toIso(a.getEndTime()) : null); // FIX (1)
        o.addProperty("bidCount", a.getBidHistory() != null ? a.getBidHistory().size() : 0);
        o.addProperty("category", i.getCategory());
        o.addProperty("imageUrl", ImageConfig.buildPublicImageUrl(i.getImageUrl()));
        return o;
    }

    private static JsonObject toAuctionDetail(Auction auction) {
        JsonObject auc = new JsonObject();
        auc.addProperty("id", auction.getId());

        Item item = auction.getItem();
        Seller seller = item.getSeller();
        //Sửa lại để khi vào lại màn hình bidding nó sẽ hiện luôn winner nếu có
        Bidder winner = auction.getCurrentWinner();

        JsonObject it = new JsonObject();
        it.addProperty("name", item.getName());
        it.addProperty("description", item.getDescription());
        it.addProperty("category", item.getCategory());
        it.addProperty("startingPrice", item.getStartingPrice());
        it.add("attributes", item.getAttributes());
        it.addProperty("sellerName", seller.getUsername());
        it.addProperty("imageUrl", item.getImageUrl());
        auc.add("item", it);

        auc.addProperty("status", auction.getStatus().name());
        auc.addProperty("currentPrice", auction.getCurrentPrice());
        auc.addProperty("endTime", TimeUtil.toIso(auction.getEndTime()));
        auc.addProperty("startTime", TimeUtil.toIso(auction.getStartTime()));
        auc.addProperty("bidCount", auction.getBidHistory() != null ? auction.getBidHistory().size() : 0);
        auc.addProperty("winnerName", winner != null ? winner.getUsername() : null);

        return auc;
    }

    private JsonObject toBidJson(BidTransaction bt) {
        JsonObject b = new JsonObject();
        b.addProperty("id",        bt.getId());
        b.addProperty("auctionId", bt.getAuctionId());

        b.addProperty("bidderId",   bt.getBidder().getId());
        b.addProperty("bidderName", bt.getBidder().getUsername());

        b.addProperty("amount",     bt.getAmount());
        b.addProperty("timestamp",  TimeUtil.toIso(bt.getCreatedAt()));
        //b.addProperty("isAutoBid",         bt.isAutoBid);
        return b;
    }

    private JsonObject toItemSummary(Item i, Auction a){
        JsonObject it = new JsonObject();
        it.addProperty("id", i.getId());
        it.addProperty("name", i.getName());
        it.addProperty("category", i.getCategory());
        it.addProperty("startingPrice", i.getStartingPrice());

        it.addProperty("auctionId",  a.getId() );
        it.addProperty("auctionStatus", a.getStatus().name());

        return it;

    }
}





