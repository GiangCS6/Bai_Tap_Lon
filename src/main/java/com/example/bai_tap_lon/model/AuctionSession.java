package com.example.bai_tap_lon.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionSession extends Entity {
    private Item item;           // bạn có thể thêm class Item nếu chưa có
    private Seller seller;
    private Bidder winner;       // null nếu chưa có người thắng
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // getters, setters, constructor...
    public AuctionSession(){
        super(0);
    }
}
