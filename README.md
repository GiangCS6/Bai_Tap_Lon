# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (Online Auction System)

## Mô tả ngắn gọn bài toán và phạm vi hệ thống

Hệ thống cho phép người dùng tham gia đấu giá các món đồ trực tuyến theo thời gian thực.  
**Phạm vi hệ thống:**
- Quản lý tài khoản (User, Seller, Admin)
- Đăng bán sản phẩm (Item)
- Tạo và quản lý phiên đấu giá (Auction)
- Đặt giá (Bidding) thời gian thực
- Theo dõi danh sách theo dõi (Watchlist)
- Quản lý ví (Wallet)
- Lịch sử đấu giá của Bidder/Seller
- Thông báo (Notification)
- Giao diện JavaFX hiện đại

Hệ thống sử dụng kiến trúc **Client-Server** với giao tiếp Socket và giao thức JSON.

## Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

**Công nghệ:**
- **Java 21+** (Maven)
- **JavaFX 21** (Giao diện client)
- **SQLite** (Cơ sở dữ liệu)
- **Gson** (Serialize/Deserialize JSON)
- **Socket Programming** (Client-Server)
- **Maven** (Build tool)

**Yêu cầu cài đặt:**
- JDK 21 trở lên
- Maven 3.6+
- (Không cần cài đặt database riêng - sử dụng file `auction.db`)
