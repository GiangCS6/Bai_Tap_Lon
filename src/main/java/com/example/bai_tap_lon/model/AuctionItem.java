package com.example.bai_tap_lon.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionItem extends Entity {
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private final User seller;
    private User leadingBidder;
    private final List<Bid> bids = new ArrayList<>();
    private final List<Integer> watcherIds = new ArrayList<>();

    public AuctionItem(
            int id,
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User seller
    ) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seller = seller;
        this.status = AuctionStatus.OPEN;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public void setCurrentHighestPrice(BigDecimal currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public User getSeller() {
        return seller;
    }

    public User getLeadingBidder() {
        return leadingBidder;
    }

    public void addBid(Bid bid) {
        bids.add(bid);
        leadingBidder = bid.getBidder();
        currentHighestPrice = bid.getAmount();
    }

    public List<Bid> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public List<Integer> getWatcherIds() {
        return Collections.unmodifiableList(watcherIds);
    }

    public boolean isWatchedBy(User user) {
        return user != null && watcherIds.contains(user.getId());
    }

    public void addWatcher(User user) {
        if (user != null && !watcherIds.contains(user.getId())) {
            watcherIds.add(user.getId());
        }
    }

    public void removeWatcher(User user) {
        if (user != null) {
            watcherIds.remove(Integer.valueOf(user.getId()));
        }
    }

    public boolean isEditableBy(User user) {
        return user != null && (user.getRole() == UserRole.ADMIN || seller.getId() == user.getId());
    }

    @Override
    public String toString() {
        return name;
    }
}
