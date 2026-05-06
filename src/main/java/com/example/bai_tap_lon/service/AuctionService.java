package com.example.bai_tap_lon.service;

import com.example.bai_tap_lon.model.Admin;
import com.example.bai_tap_lon.model.AuctionItem;
import com.example.bai_tap_lon.model.AuctionStatus;
import com.example.bai_tap_lon.model.Bid;
import com.example.bai_tap_lon.model.Bidder;
import com.example.bai_tap_lon.model.Seller;
import com.example.bai_tap_lon.model.User;
import com.example.bai_tap_lon.model.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionService {
    private int nextUserId = 1;
    private int nextItemId = 1;
    private final List<User> users = new ArrayList<>();
    private final List<AuctionItem> items = new ArrayList<>();

    public AuctionService() {
    }

    public User login(String username, String password) throws AuctionException {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                .filter(user -> user.hasPassword(password))
                .findFirst()
                .orElseThrow(() -> new AuctionException("Sai ten dang nhap hoac mat khau."));
    }

    public User register(String username, String password, String fullName, UserRole role) throws AuctionException {
        requireText(username, "Ten dang nhap");
        requireText(password, "Mat khau");
        requireText(fullName, "Ho ten");

        boolean exists = users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username.trim()));
        if (exists) {
            throw new AuctionException("Ten dang nhap da ton tai.");
        }

        User user = switch (role) {
            case ADMIN -> new Admin(nextUserId++, username.trim(), password, fullName.trim());
            case SELLER -> new Seller(nextUserId++, username.trim(), password, fullName.trim());
            case BIDDER -> new Bidder(nextUserId++, username.trim(), password, fullName.trim());
        };
        users.add(user);
        return user;
    }

    public List<AuctionItem> getItems() {
        updateStatuses();
        return new ArrayList<>(items);
    }

    public AuctionItem addItem(
            User seller,
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws AuctionException {
        requireSeller(seller);
        validateItem(name, startingPrice, startTime, endTime);
        AuctionItem item = new AuctionItem(nextItemId++, name.trim(), description.trim(), startingPrice, startTime, endTime, seller);
        items.add(item);
        updateStatus(item, LocalDateTime.now());
        return item;
    }

    public void updateItem(
            User actor,
            AuctionItem item,
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws AuctionException {
        requireItem(item);
        if (!item.isEditableBy(actor)) {
            throw new AuctionException("Ban khong co quyen sua san pham nay.");
        }
        if (!item.getBids().isEmpty()) {
            throw new AuctionException("Khong the sua san pham da co nguoi dau gia.");
        }

        validateItem(name, startingPrice, startTime, endTime);
        item.setName(name.trim());
        item.setDescription(description.trim());
        item.setStartingPrice(startingPrice);
        item.setCurrentHighestPrice(startingPrice);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setStatus(AuctionStatus.OPEN);
        updateStatus(item, LocalDateTime.now());
    }

    public void deleteItem(User actor, AuctionItem item) throws AuctionException {
        requireItem(item);
        if (!item.isEditableBy(actor)) {
            throw new AuctionException("Ban khong co quyen xoa san pham nay.");
        }
        if (!item.getBids().isEmpty()) {
            throw new AuctionException("Khong the xoa san pham da co nguoi dau gia.");
        }
        items.remove(item);
    }

    public void placeBid(User bidder, AuctionItem item, BigDecimal amount) throws AuctionException {
        requireItem(item);
        if (bidder == null || bidder.getRole() != UserRole.BIDDER) {
            throw new AuctionException("Chi tai khoan Bidder moi duoc dat gia.");
        }
        if (item.getSeller().getId() == bidder.getId()) {
            throw new AuctionException("Nguoi ban khong duoc dau gia san pham cua minh.");
        }

        updateStatus(item, LocalDateTime.now());
        if (item.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionException("Phien dau gia khong o trang thai RUNNING.");
        }
        if (amount == null || amount.compareTo(item.getCurrentHighestPrice()) <= 0) {
            throw new AuctionException("Gia dau phai cao hon gia hien tai.");
        }

        item.addBid(new Bid(bidder, amount, LocalDateTime.now()));
    }

    public void markPaid(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        updateStatus(item, LocalDateTime.now());
        if (item.getStatus() != AuctionStatus.FINISHED) {
            throw new AuctionException("Chi phien FINISHED moi co the chuyen sang PAID.");
        }
        item.setStatus(AuctionStatus.PAID);
    }

    public void cancel(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        if (item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionException("Khong the huy phien da thanh toan.");
        }
        item.setStatus(AuctionStatus.CANCELED);
    }

    public Optional<User> getWinner(AuctionItem item) {
        updateStatus(item, LocalDateTime.now());
        return Optional.ofNullable(item.getLeadingBidder());
    }

    private void updateStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (AuctionItem item : items) {
            updateStatus(item, now);
        }
    }

    private void updateStatus(AuctionItem item, LocalDateTime now) {
        if (item.getStatus() == AuctionStatus.CANCELED || item.getStatus() == AuctionStatus.PAID) {
            return;
        }
        if (!now.isBefore(item.getEndTime())) {
            item.setStatus(AuctionStatus.FINISHED);
        } else if (!now.isBefore(item.getStartTime())) {
            item.setStatus(AuctionStatus.RUNNING);
        } else {
            item.setStatus(AuctionStatus.OPEN);
        }
    }

    private void validateItem(String name, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime)
            throws AuctionException {
        requireText(name, "Ten san pham");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuctionException("Gia khoi diem phai lon hon 0.");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new AuctionException("Thoi gian ket thuc phai sau thoi gian bat dau.");
        }
    }

    private void requireSeller(User seller) throws AuctionException {
        if (seller == null || seller.getRole() != UserRole.SELLER) {
            throw new AuctionException("Chi tai khoan Seller moi duoc quan ly san pham.");
        }
    }

    private void requireAdmin(User actor) throws AuctionException {
        if (actor == null || actor.getRole() != UserRole.ADMIN) {
            throw new AuctionException("Chi Admin moi duoc thuc hien thao tac nay.");
        }
    }

    private void requireItem(AuctionItem item) throws AuctionException {
        if (item == null) {
            throw new AuctionException("Vui long chon mot phien dau gia.");
        }
    }

    private void requireText(String value, String fieldName) throws AuctionException {
        if (value == null || value.trim().isEmpty()) {
            throw new AuctionException(fieldName + " khong duoc de trong.");
        }
    }
}
