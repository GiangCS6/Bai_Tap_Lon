package com.example.bai_tap_lon.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private final UserRole originalRole;

    public Admin(int id, String username, String password, String fullName) {
        this(id, username, password, fullName, false, UserRole.BIDDER);
    }

    public Admin(int id, String username, String password, String fullName, boolean locked) {
        this(id, username, password, fullName, locked, UserRole.BIDDER);
    }


    public boolean banUser(User targetUser, String reason) {
        if (targetUser instanceof Admin) {
            System.out.println("Không thể ban một Admin khác!");
            return false;
        }
        // TODO: Cập nhật trạng thái của targetUser thành BANNED trong Database (DAO)
        System.out.println("Đã khóa tài khoản: " + targetUser.getUsername() + " | Lý do: " + reason);
        return true;
    }


    public boolean cancelAuction(String auctionId, String reason) {
        // TODO: Gọi logic kiểm tra Auction từ Database
        // Cập nhật trạng thái phiên đấu giá thành CANCELED
        System.out.println("Phiên đấu giá " + auctionId + " đã bị Admin hủy. Lý do: " + reason);
        return true;
    }

    private UserRole normalizeOriginalRole(UserRole role) {
        if (role == null || role == UserRole.ADMIN) {
            return UserRole.BIDDER;
        }
        return role;
    }
}
