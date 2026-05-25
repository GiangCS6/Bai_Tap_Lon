package bai_tap_lon.server.network;

import bai_tap_lon.common.model.entity.Auction;
import bai_tap_lon.common.model.entity.AuctionLifecycleListener;
import bai_tap_lon.common.model.entity.BidObserver;
import bai_tap_lon.common.model.entity.BidTransaction;
import bai_tap_lon.common.model.user.Bidder;
import bai_tap_lon.common.model.user.User;
import bai_tap_lon.common.network.Push;
import bai_tap_lon.common.network.Request;
import bai_tap_lon.common.network.Response;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable, BidObserver{
    private final ServerStart server;
    private final RequestRouter requestRouter;
    private final Socket client;
    private final Gson gson;

    private BufferedReader reader;
    private BufferedWriter writer;

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Set<String> watchingAuctionId = ConcurrentHashMap.newKeySet();
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());


    private User user; //TODO: sau khi login thi set user, de push notification cho dung doi tuong user

    /**
     * Listener cho lifecycle event toàn manager (lobby broadcast).
     * Tách khỏi BidObserver vì:
     *   - BidObserver = subscription per-auction (đăng ký qua Auction.addObserver)
     *   - LifecycleListener = subscription global (đăng ký qua AuctionManager)
     * Hai mục đích khác nhau → composition cleaner hơn nhồi vào 1 class
     * (cũng tránh xung đột signature onAuctionStarted/onAuctionCancelled
     * giữa hai interface).
     */
    private final AuctionLifecycleListener globalLifecycleObserver = new AuctionLifecycleListener() {
        @Override
        public void onAuctionCreated(Auction auction) {
            sendLifecyclePush("AUCTION_CREATED_ALL", auction);
        }
        @Override
        public void onAuctionStarted(Auction auction) {
            sendLifecyclePush("AUCTION_STARTED_ALL", auction);
        }
        @Override
        public void onAuctionEnded(Auction auction) {
            sendLifecyclePush("AUCTION_ENDED_ALL", auction);
        }
        @Override
        public void onAuctionCancelled(Auction auction, String reason) {
            sendLifecyclePush("AUCTION_CANCELLED_ALL", auction);
        }
    };

    public ClientHandler(Socket client,ServerStart server, RequestRouter requestRouter) throws IOException {
        this.client = client;
        this.server = server;
        this.requestRouter = requestRouter;
        this.gson = server.getGson();
        this.reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));

        // Tự subscribe vào lifecycle event toàn manager — nhận push lobby
        server.getAuctionManager().addLifecycleListener(globalLifecycleObserver);
    }

    ///  After submit to pool
    @Override
    public void run() {
        try{
            String line;
            while(active.get() && (line = reader.readLine()) != null ){
                Response response;
                try {
                    Request request = RequestParser.parseRequest(line);
                    response = requestRouter.route(request, this);
                    sendMessage(gson.toJson(response));
                } catch (IllegalArgumentException ie){
                    logger.log(Level.WARNING, "Invalid request: ", ie);
                }
            }
        }
        catch (IOException e) {
            logger.log(Level.SEVERE,"IO error with client: ",e);
        } finally {
            disconnect();
            server.removeClient(this);
        }
    }

    /// Send message to client
    public synchronized void sendMessage(String json){
        logger.info("[Debug sent] "+json);
        try{
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            active.set(false);
            disconnect();
        }
    }

    ///_____DISCONNECT____
    public void disconnect(){
        //check neu nhieu luong cung goi disconnect
        // compareAndSet: check currentValue vs expected value
        if (!active.compareAndSet(true,false)) return;

        // Unsubscribe khỏi lifecycle global
        server.getAuctionManager().removeLifecycleListener(globalLifecycleObserver);

        //remove client from observer auction
        for(String auctionid: watchingAuctionId){
            Auction auction = server.getAuctionManager().getAuction(auctionid);
            if (auction != null){
                auction.removeObserver(this);
            }
        }
        // close stream and socket
        try {
            client.close();
        } catch (IOException ignored) {
        }
    }

    /// get,set User
    public User getUser() {
        return user;
    }
    public void setUser(User user){ this.user = user;}

    /// ______WATCHING AUCTION MANAGEMENT______
    public void addWatchingAuction(String auctionId) {
        watchingAuctionId.add(auctionId);
    }

    public void removeWatchingAuction(String auctionId) {
        watchingAuctionId.remove(auctionId);
    }


    ///______IMPLEMENT BIDOBSERVER______
    // BIDOBSERVER chỉ thông báo cho các user khác, không liên quan gì đến logic
    @Override
    public void onBidPlaced(BidTransaction bidTransaction){
        JsonObject data = new JsonObject();
        JsonObject b = new JsonObject();
        b.addProperty("id",        bidTransaction.getId());
        b.addProperty("auctionId", bidTransaction.getAuctionId());

        b.addProperty("bidderId",   bidTransaction.getBidder().getId());
        b.addProperty("bidderName", bidTransaction.getBidder().getUsername());

        b.addProperty("amount",     bidTransaction.getAmount());
        b.addProperty("timestamp",  TimeUtil.toIso(bidTransaction.getCreatedAt()));

        data.addProperty("auctionId", bidTransaction.getAuctionId());
        data.add("transaction", b);

        Push push = Push.build("BID_PLACED", data);
        sendMessage(gson.toJson(push));
    }
    public void onAuctionStarted(Auction auction){
        JsonObject data = new JsonObject();
        data.addProperty("auctionId",     auction.getId());
        data.addProperty("startingPrice", auction.getItem().getStartingPrice());
        data.addProperty("startTime",     TimeUtil.toIso(auction.getStartTime()));
        data.addProperty("endTime",       TimeUtil.toIso(auction.getEndTime()));

        Push push = Push.build("AUCTION_STARTED", data);
        sendMessage(gson.toJson(push));
    }

    public void onAuctionEnd(Auction auction){
        Bidder winner = auction.getWinner();

        JsonObject data = new JsonObject();
        data.addProperty("auctionId",  auction.getId());
        data.addProperty("finalPrice", auction.getCurrentPrice());
        data.addProperty("status",     auction.getStatus().name());


        if (winner != null) {
            data.addProperty("winnerName", winner.getUsername());
        } else {
            data.add("winnerName", JsonNull.INSTANCE);
        }
        Push push = Push.build("AUCTION_ENDED", data);
        sendMessage(gson.toJson(push));
    }

    public void onTimeExtended(Auction auction){
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auction.getId());
        data.addProperty("newEndTime", TimeUtil.toIso(auction.getEndTime()));

        Push push = Push.build("AUCTION_TIME_EXTENDED", data);
        sendMessage(gson.toJson(push));
    }

    public void onAuctionCancelled(Auction auction, String reason){
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auction.getId());
        data.addProperty("reason", reason);

        Push push = Push.build("AUCTION_CANCELLED", data);
        sendMessage(gson.toJson(push));
    }

    /// _____GLOBAL LIFECYCLE PUSH HELPER_____
    /** Gửi push lifecycle (lobby) với payload là auction summary. */
    private void sendLifecyclePush(String action, Auction auction) {
        JsonObject summary = RequestRouter.toAuctionSummary(auction);
        Push push = Push.build(action, summary);
        sendMessage(gson.toJson(push));
    }
}