package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private String adminLevel; // Ví dụ: "SUPER_ADMIN", "MODERATOR"
    private LocalDateTime lastLogin;

    public Admin(String username, String password, String email, String adminLevel) {
        super(username, password, email);
        this.adminLevel = adminLevel;
    }

    public String getAdminLevel() { return adminLevel; }
    public void setAdminLevel(String adminLevel) { this.adminLevel = adminLevel; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    @Override
    public void displayRoleInfo() {
        System.out.println("Role: Admin | Level: " + adminLevel + " | Username: " + getUsername());
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
}
