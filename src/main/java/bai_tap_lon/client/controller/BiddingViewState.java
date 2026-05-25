package bai_tap_lon.client.controller;

import java.time.LocalDateTime;

// Giữ các biến dùng chung của bidding => Controller đỡ phải ôm nhiều biến rời rạc
public class BiddingViewState {
    public String auctionId;
    public String status = "";
    public long currentPrice = 0L;
    public long availableBalance = 0L;
    public boolean autoBidActive = false;
    public LocalDateTime endTime;
}