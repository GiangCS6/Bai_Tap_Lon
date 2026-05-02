package com.example.bai_tap_lon.network;

public class AuctionManager {
    // [KIẾN THỨC: Singleton Pattern]
    private static AuctionManager instance;

    // Giả sử đây là giá hiện tại của sản phẩm đang đấu giá
    private double currentHighestBid = 0;
    private String currentWinner = "Chưa có";

    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /**
     * [KIẾN THỨC BẮT BUỘC: Xử lý Concurrent Bidding an toàn]
     * Từ khóa 'synchronized' đảm bảo rằng tại 1 thời điểm (millisecond),
     * chỉ có MỘT luồng (Thread) được phép vào hàm này.
     * Ngăn chặn hoàn toàn lỗi Lost Update khi 2 người cùng bấm đặt giá.
     */
    public synchronized boolean placeBid(String username, double bidAmount) {
        if (bidAmount > currentHighestBid) {
            // Cập nhật giá mới
            currentHighestBid = bidAmount;
            currentWinner = username;
            System.out.println(">>> " + username + " ĐÃ ĐẶT GIÁ THÀNH CÔNG: " + bidAmount);
            return true; // Đặt giá hợp lệ
        } else {
            return false; // Đặt giá thấp hơn giá hiện tại -> Bị từ chối
        }
    }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getCurrentWinner() { return currentWinner; }
}