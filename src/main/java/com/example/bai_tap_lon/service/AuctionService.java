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
        loadData();
    }

    /**
     * Tải dữ liệu từ file
     */
    private void loadData() {
        try {
            // Load users
            DataPersistence.UserLoadData userData = DataPersistence.loadUsers();
            users.addAll(userData.users);
            nextUserId = userData.nextUserId;

            // Load items
            DataPersistence.ItemLoadData itemData = DataPersistence.loadItems(users);
            items.addAll(itemData.items);
            nextItemId = itemData.nextItemId;
        } catch (Exception e) {
            System.err.println("Lỗi khi tải dữ liệu: " + e.getMessage());
            // Nếu có lỗi, tiếp tục với dữ liệu trống
        }
    }

    /**
     * Lưu dữ liệu vào file
     */
    private void saveData() {
        try {
            DataPersistence.saveUsers(users, nextUserId);
            DataPersistence.saveItems(items, nextItemId);
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu dữ liệu: " + e.getMessage());
        }
    }

    public User login(String username, String password) throws AuctionException {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                .filter(user -> user.hasPassword(password))
                .findFirst()
                .orElseThrow(() -> new AuctionException("Sai tên đăng nhập hoặc mật khẩu."));
    }

    public User register(String username, String password, String fullName, UserRole role) throws AuctionException {
        requireText(username, "Tên đăng nhập");
        requireText(password, "Mật khẩu");
        requireText(fullName, "Họ tên");

        boolean exists = users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username.trim()));
        if (exists) {
            throw new AuctionException("Tên đăng nhập đã tồn tại.");
        }

        User user = switch (role) {
            case ADMIN -> new Admin(nextUserId++, username.trim(), password, fullName.trim());
            case SELLER -> new Seller(nextUserId++, username.trim(), password, fullName.trim());
            case BIDDER -> new Bidder(nextUserId++, username.trim(), password, fullName.trim());
        };
        users.add(user);
        saveData();
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
        saveData();
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
            throw new AuctionException("Bạn không có quyền sửa sản phẩm này.");
        }
        if (!item.getBids().isEmpty()) {
            throw new AuctionException("Không thể sửa sản phẩm đã có người đấu giá.");
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
        saveData();
    }

    public void deleteItem(User actor, AuctionItem item) throws AuctionException {
        requireItem(item);
        if (!item.isEditableBy(actor)) {
            throw new AuctionException("Bạn không có quyền xóa sản phẩm này.");
        }
        if (!item.getBids().isEmpty()) {
            throw new AuctionException("Không thể xóa sản phẩm đã có người đấu giá.");
        }
        items.remove(item);
        saveData();
    }

    public void placeBid(User bidder, AuctionItem item, BigDecimal amount) throws AuctionException {
        requireItem(item);
        if (bidder == null || bidder.getRole() != UserRole.BIDDER) {
            throw new AuctionException("Chỉ tài khoản người đấu giá mới được đặt giá.");
        }
        if (item.getSeller().getId() == bidder.getId()) {
            throw new AuctionException("Người bán không được đấu giá sản phẩm của mình.");
        }

        updateStatus(item, LocalDateTime.now());
        if (item.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionException("Phiên đấu giá không ở trạng thái đang diễn ra.");
        }
        if (amount == null || amount.compareTo(item.getCurrentHighestPrice()) <= 0) {
            throw new AuctionException("Không thể đặt giá sản phẩm thấp hơn giá hiện tại");
        }

        item.addBid(new Bid(bidder, amount, LocalDateTime.now()));
        saveData();
    }

    public void markPaid(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        updateStatus(item, LocalDateTime.now());
        if (item.getStatus() != AuctionStatus.FINISHED) {
            throw new AuctionException("Chỉ phiên đã kết thúc mới có thể chuyển sang đã thanh toán.");
        }
        item.setStatus(AuctionStatus.PAID);
        saveData();
    }

    public void cancel(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        if (item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionException("Không thể hủy phiên đã thanh toán.");
        }
        item.setStatus(AuctionStatus.CANCELED);
        saveData();
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
        requireText(name, "Tên sản phẩm");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuctionException("Giá khởi điểm phải lớn hơn 0.");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new AuctionException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    private void requireSeller(User seller) throws AuctionException {
        if (seller == null || seller.getRole() != UserRole.SELLER) {
            throw new AuctionException("Chỉ tài khoản người bán mới được quản lý sản phẩm.");
        }
    }

    private void requireAdmin(User actor) throws AuctionException {
        if (actor == null || actor.getRole() != UserRole.ADMIN) {
            throw new AuctionException("Chỉ quản trị viên mới được thực hiện thao tác này.");
        }
    }

    private void requireItem(AuctionItem item) throws AuctionException {
        if (item == null) {
            throw new AuctionException("Vui lòng chọn một phiên đấu giá.");
        }
    }

    private void requireText(String value, String fieldName) throws AuctionException {
        if (value == null || value.trim().isEmpty()) {
            throw new AuctionException(fieldName + " không được để trống.");
        }
    }
}
