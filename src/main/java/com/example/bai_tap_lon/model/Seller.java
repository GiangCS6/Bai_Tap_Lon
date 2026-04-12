package com.example.bai_tap_lon.model;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private String storeName; // Tên cửa hàng/thương hiệu của người bán
    private List<String> listedItemIds; // Danh sách ID các sản phẩm đã đăng

    public Seller(String username, String password, String email, String storeName) {
        super(username, password, email);
        this.storeName = storeName;
        this.listedItemIds = new ArrayList<>();
    }

    // Các Getter và Setter
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public List<String> getListedItemIds() { return listedItemIds; }

    @Override
    public void displayRoleInfo() {
        System.out.println("Vai trò: Người bán | Cửa hàng: " + storeName + " | Chủ sở hữu: " + getUsername());
    }

    // --- LOGIC NGHIỆP VỤ CỦA SELLER ---

    /**
     * Thêm sản phẩm mới vào danh sách quản lý của người bán
     */
    public void addNewItem(String itemId) {
        this.listedItemIds.add(itemId);
    }

    /**
     * Xóa sản phẩm khỏi danh sách (ví dụ khi sản phẩm bị hủy)
     */
    public void removeItem(String itemId) {
        this.listedItemIds.remove(itemId);
    }
}
