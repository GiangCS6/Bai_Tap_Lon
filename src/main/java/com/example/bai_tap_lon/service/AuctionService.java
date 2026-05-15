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
    private static final String ROOT_ADMIN_USERNAME = "admin";
    private static final String ROOT_ADMIN_PASSWORD = "Dung12345!!!";

    private int nextUserId = 1;
    private int nextItemId = 1;
    private final List<User> users = new ArrayList<>();
    private final List<AuctionItem> items = new ArrayList<>();

    public AuctionService() {
        loadData();
    }

    private void loadData() {
        try {
            DataPersistence.UserLoadData userData = DataPersistence.loadUsers();
            users.addAll(userData.users);
            nextUserId = userData.nextUserId;
            boolean userDataChanged = ensureRootAdminAccount();

            DataPersistence.ItemLoadData itemData = DataPersistence.loadItems(users);
            items.addAll(itemData.items);
            nextItemId = itemData.nextItemId;

            if (userDataChanged) {
                saveData();
            }
        } catch (Exception e) {
            System.err.println("Loi khi tai du lieu: " + e.getMessage());
        }
    }

    private void saveData() {
        try {
            DataPersistence.saveUsers(users, nextUserId);
            DataPersistence.saveItems(items, nextItemId);
        } catch (Exception e) {
            System.err.println("Loi khi luu du lieu: " + e.getMessage());
        }
    }

    public User login(String username, String password) throws AuctionException {
        requireText(username, "Ten dang nhap");
        requireText(password, "Mat khau");

        User user = users.stream()
                .filter(account -> account.getUsername().equalsIgnoreCase(username.trim()))
                .filter(account -> account.hasPassword(password))
                .findFirst()
                .orElseThrow(() -> new AuctionException("Sai ten dang nhap hoac mat khau."));

        if (user.isLocked()) {
            throw new AuctionException("Tai khoan dang bi khoa.");
        }
        return user;
    }

    public User register(String username, String password, String fullName, UserRole role) throws AuctionException {
        requireText(username, "Ten dang nhap");
        requireText(password, "Mat khau");
        requireText(fullName, "Ho ten");
        if (role == null) {
            throw new AuctionException("Vui long chon vai tro tai khoan.");
        }
        if (role == UserRole.ADMIN) {
            throw new AuctionException("Khong the dang ky truc tiep tai khoan admin.");
        }

        boolean exists = users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username.trim()));
        if (exists) {
            throw new AuctionException("Ten dang nhap da ton tai.");
        }

        User user = switch (role) {
            case SELLER -> new Seller(nextUserId++, username.trim(), password, fullName.trim());
            case BIDDER -> new Bidder(nextUserId++, username.trim(), password, fullName.trim());
            case ADMIN -> throw new AuctionException("Khong the dang ky truc tiep tai khoan admin.");
        };
        users.add(user);
        saveData();
        return user;
    }

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public boolean isRootAdmin(User user) {
        return user != null
                && user.getRole() == UserRole.ADMIN
                && user.getUsername().equalsIgnoreCase(ROOT_ADMIN_USERNAME)
                && user.hasPassword(ROOT_ADMIN_PASSWORD);
    }

    public void grantAdmin(User actor, User target) throws AuctionException {
        requireRootAdmin(actor);
        User storedUser = requireStoredUser(target);
        if (isRootAdmin(storedUser)) {
            throw new AuctionException("Tai khoan admin goc da co quyen admin.");
        }
        if (storedUser.getRole() == UserRole.ADMIN) {
            throw new AuctionException("Tai khoan nay da la admin.");
        }

        int index = findUserIndexById(storedUser.getId());
        users.set(index, new Admin(
                storedUser.getId(),
                storedUser.getUsername(),
                storedUser.getPassword(),
                storedUser.getFullName(),
                storedUser.isLocked()
        ));
        saveData();
    }

    public void deleteUser(User actor, User target) throws AuctionException {
        requireAdmin(actor);
        User storedUser = requireStoredUser(target);
        if (isRootAdmin(storedUser)) {
            throw new AuctionException("Khong the xoa tai khoan admin goc.");
        }
        if (actor.getId() == storedUser.getId()) {
            throw new AuctionException("Khong the xoa tai khoan dang dang nhap.");
        }

        users.removeIf(user -> user.getId() == storedUser.getId());
        saveData();
    }

    public void setUserLocked(User actor, User target, boolean locked) throws AuctionException {
        requireAdmin(actor);
        User storedUser = requireStoredUser(target);
        if (isRootAdmin(storedUser)) {
            throw new AuctionException("Khong the khoa tai khoan admin goc.");
        }
        if (actor.getId() == storedUser.getId()) {
            throw new AuctionException("Khong the khoa tai khoan dang dang nhap.");
        }

        storedUser.setLocked(locked);
        saveData();
    }

    public List<AuctionItem> getItems() {
        if (updateStatuses()) {
            saveData();
        }
        return new ArrayList<>(items);
    }

    public void setItemWatched(User bidder, AuctionItem item, boolean watched) throws AuctionException {
        requireItem(item);
        if (bidder == null || bidder.getRole() != UserRole.BIDDER) {
            throw new AuctionException("Chi tai khoan nguoi dau gia moi duoc theo doi san pham.");
        }
        if (bidder.isLocked()) {
            throw new AuctionException("Tai khoan dang bi khoa.");
        }

        boolean alreadyWatched = item.isWatchedBy(bidder);
        if (watched && !alreadyWatched) {
            item.addWatcher(bidder);
            saveData();
        } else if (!watched && alreadyWatched) {
            item.removeWatcher(bidder);
            saveData();
        }
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
            throw new AuctionException("Ban khong co quyen sua san pham nay.");
        }
        if (!item.getBids().isEmpty()) {
            throw new AuctionException("Khong the sua san pham da co nguoi dau gia.");
        }

        updateItemFields(item, name, description, startingPrice, startTime, endTime);
        saveData();
    }

    public void adminUpdateItem(
            User actor,
            AuctionItem item,
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        if (!item.getBids().isEmpty() && startingPrice.compareTo(item.getStartingPrice()) != 0) {
            throw new AuctionException("San pham da co nguoi dau gia nen khong the sua gia khoi diem.");
        }
        updateItemFields(item, name, description, startingPrice, startTime, endTime);
        saveData();
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
        saveData();
    }

    public void adminDeleteItem(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        items.removeIf(existing -> existing.getId() == item.getId());
        saveData();
    }

    public void placeBid(User bidder, AuctionItem item, BigDecimal amount) throws AuctionException {
        requireItem(item);
        if (bidder == null || bidder.getRole() != UserRole.BIDDER) {
            throw new AuctionException("Chi tai khoan nguoi dau gia moi duoc dat gia.");
        }
        if (bidder.isLocked()) {
            throw new AuctionException("Tai khoan dang bi khoa.");
        }
        if (item.getSeller().getId() == bidder.getId()) {
            throw new AuctionException("Nguoi ban khong duoc dau gia san pham cua minh.");
        }

        updateStatus(item, LocalDateTime.now());
        if (item.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionException("Phien dau gia khong o trang thai dang dien ra.");
        }
        if (amount == null || amount.compareTo(item.getCurrentHighestPrice()) <= 0) {
            throw new AuctionException("Gia dat phai cao hon gia hien tai.");
        }

        item.addBid(new Bid(bidder, amount, LocalDateTime.now()));
        saveData();
    }

    public void markPaid(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        updateStatus(item, LocalDateTime.now());
        if (item.getStatus() != AuctionStatus.FINISHED) {
            throw new AuctionException("Chi phien da ket thuc moi co the chuyen sang da thanh toan.");
        }
        item.setStatus(AuctionStatus.PAID);
        saveData();
    }

    public void cancel(User actor, AuctionItem item) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        if (item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionException("Khong the huy phien da thanh toan.");
        }
        item.setStatus(AuctionStatus.CANCELED);
        saveData();
    }

    public void extendAuction(User actor, AuctionItem item, LocalDateTime newEndTime) throws AuctionException {
        requireAdmin(actor);
        requireItem(item);
        if (newEndTime == null) {
            throw new AuctionException("Vui long nhap thoi gian ket thuc moi.");
        }
        if (item.getStatus() == AuctionStatus.CANCELED || item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionException("Khong the gia han phien da huy hoac da thanh toan.");
        }
        if (!newEndTime.isAfter(LocalDateTime.now())) {
            throw new AuctionException("Thoi gian ket thuc moi phai sau thoi diem hien tai.");
        }
        if (!newEndTime.isAfter(item.getEndTime())) {
            throw new AuctionException("Thoi gian ket thuc moi phai sau thoi gian ket thuc hien tai.");
        }

        item.setEndTime(newEndTime);
        updateStatus(item, LocalDateTime.now());
        saveData();
    }

    public Optional<User> getWinner(AuctionItem item) {
        updateStatus(item, LocalDateTime.now());
        return Optional.ofNullable(item.getLeadingBidder());
    }

    private void updateItemFields(
            AuctionItem item,
            String name,
            String description,
            BigDecimal startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws AuctionException {
        validateItem(name, startingPrice, startTime, endTime);
        item.setName(name.trim());
        item.setDescription(description.trim());
        item.setStartingPrice(startingPrice);
        if (item.getBids().isEmpty()) {
            item.setCurrentHighestPrice(startingPrice);
        }
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        updateStatus(item, LocalDateTime.now());
    }

    private boolean updateStatuses() {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (AuctionItem item : items) {
            changed |= updateStatus(item, now);
        }
        return changed;
    }

    private boolean updateStatus(AuctionItem item, LocalDateTime now) {
        if (item.getStatus() == AuctionStatus.CANCELED || item.getStatus() == AuctionStatus.PAID) {
            return false;
        }

        AuctionStatus nextStatus;
        if (!now.isBefore(item.getEndTime())) {
            nextStatus = AuctionStatus.FINISHED;
        } else if (!now.isBefore(item.getStartTime())) {
            nextStatus = AuctionStatus.RUNNING;
        } else {
            nextStatus = AuctionStatus.OPEN;
        }

        if (item.getStatus() == nextStatus) {
            return false;
        }
        item.setStatus(nextStatus);
        return true;
    }

    private void validateItem(String name, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime)
            throws AuctionException {
        requireText(name, "Ten san pham");
        if (descriptionIsMissing(startingPrice)) {
            throw new AuctionException("Gia khoi diem phai lon hon 0.");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new AuctionException("Thoi gian ket thuc phai sau thoi gian bat dau.");
        }
    }

    private boolean descriptionIsMissing(BigDecimal startingPrice) {
        return startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0;
    }

    private void requireSeller(User seller) throws AuctionException {
        if (seller == null || seller.getRole() != UserRole.SELLER) {
            throw new AuctionException("Chi tai khoan nguoi ban moi duoc quan ly san pham.");
        }
        if (seller.isLocked()) {
            throw new AuctionException("Tai khoan dang bi khoa.");
        }
    }

    private void requireAdmin(User actor) throws AuctionException {
        if (actor == null || actor.getRole() != UserRole.ADMIN) {
            throw new AuctionException("Chi quan tri vien moi duoc thuc hien thao tac nay.");
        }
        if (actor.isLocked()) {
            throw new AuctionException("Tai khoan dang bi khoa.");
        }
    }

    private void requireRootAdmin(User actor) throws AuctionException {
        if (!isRootAdmin(actor)) {
            throw new AuctionException("Chi tai khoan admin goc moi duoc cap quyen admin.");
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

    private boolean ensureRootAdminAccount() {
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (!user.getUsername().equalsIgnoreCase(ROOT_ADMIN_USERNAME)) {
                continue;
            }

            if (isRootAdmin(user) && user.getUsername().equals(ROOT_ADMIN_USERNAME) && !user.isLocked()) {
                return false;
            }

            users.set(i, new Admin(
                    user.getId(),
                    ROOT_ADMIN_USERNAME,
                    ROOT_ADMIN_PASSWORD,
                    user.getFullName(),
                    false
            ));
            return true;
        }

        users.add(new Admin(nextUserId++, ROOT_ADMIN_USERNAME, ROOT_ADMIN_PASSWORD, "Root admin"));
        return true;
    }

    private User requireStoredUser(User target) throws AuctionException {
        if (target == null) {
            throw new AuctionException("Vui long chon mot tai khoan.");
        }
        return users.stream()
                .filter(user -> user.getId() == target.getId())
                .findFirst()
                .orElseThrow(() -> new AuctionException("Khong tim thay tai khoan."));
    }

    private int findUserIndexById(int userId) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == userId) {
                return i;
            }
        }
        return -1;
    }
}
