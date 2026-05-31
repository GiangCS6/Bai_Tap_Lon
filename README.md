# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)

## 1. Giới thiệu

Hệ thống Đấu giá Trực tuyến (Online Auction System) là một ứng dụng Client-Server cho phép người dùng tham gia đấu giá các vật phẩm theo thời gian thực thông qua kết nối Socket và giao thức JSON.

Hệ thống hỗ trợ:

* Quản lý tài khoản với nhiều vai trò: User, Seller, Admin.
* Đăng bán sản phẩm và quản lý phiên đấu giá.
* Đấu giá thời gian thực (Real-time Bidding).
* Quản lý ví điện tử.
* Danh sách theo dõi sản phẩm (Watchlist).
* Lịch sử đấu giá.
* Hệ thống thông báo.
* Giao diện hiện đại xây dựng bằng JavaFX.

---

## 2. Công nghệ sử dụng

| Thành phần              | Công nghệ               |
| ----------------------- | ----------------------- |
| Ngôn ngữ lập trình      | Java 21+                |
| Build Tool              | Maven 3.6+              |
| Giao diện               | JavaFX 21               |
| Cơ sở dữ liệu           | MySQL                   |
| Giao tiếp Client-Server | Socket Programming      |
| Định dạng dữ liệu       | JSON                    |
| Thư viện JSON           | Gson                    |
| Môi trường chạy         | Windows / Linux / macOS |

---

## 3. Cấu trúc dự án

```text
.
├── .github/
│   └── workflows/
├── data/
├── server/
│   └── uploads/
├── src/
│   └── main/
├── auction.db
├── pom.xml
└── target/
```

### Mô tả

| Thư mục/File        | Chức năng                    |
| ------------------- | ---------------------------- |
| `.github/workflows` | Cấu hình CI/CD               |
| `data`              | Chứa dữ liệu hệ thống        |
| `server/uploads`    | Lưu hình ảnh và file tải lên |
| `src/main`          | Mã nguồn Server và Client    |
| `auction.db`        | Cơ sở dữ liệu SQLite         |
| `pom.xml`           | Quản lý dependency Maven     |
| `target`            | Kết quả build                |

---

## 4. Các file JAR sau khi build

### File JAR chính

| Tên file                                  | Mô tả              |
| ----------------------------------------- | ------------------ |
| `Bai_Tap_Lon-1.0-SNAPSHOT.jar`            | JAR thông thường   |
| `Bai_Tap_Lon-1.0-SNAPSHOT-client-fat.jar` | Fat JAR cho Client |
| `Bai_Tap_Lon-1.0-SNAPSHOT-server-fat.jar` | Fat JAR cho Server |

### Maven Wrapper

| Tên file            | Vị trí                           |
| ------------------- | -------------------------------- |
| `maven-wrapper.jar` | `.mvn/wrapper/maven-wrapper.jar` |

---

## 5. Thư viện Runtime

Các thư viện được Maven tự động tải về và đóng gói vào Fat JAR:

* sqlite-jdbc
* mysql-connector-j
* protobuf-java
* gson
* javafx-base
* javafx-controls
* javafx-fxml
* javafx-graphics

Sau khi đóng gói bằng `maven-shade-plugin`, toàn bộ thư viện được gộp trực tiếp vào file Fat JAR.

---

## 6. Thư viện Unit Test

Các thư viện chỉ phục vụ kiểm thử:

* junit-jupiter-api
* junit-jupiter-engine
* junit-platform-commons
* junit-platform-engine
* opentest4j
* apiguardian-api

Các thư viện này **không được đóng gói** vào Fat JAR runtime.

---

## 7. Build dự án

### Build bằng Maven

```bash
mvn clean package
```

Hoặc:

```bash
mvn clean install
```

Sau khi build thành công, các file thực thi sẽ xuất hiện trong thư mục:

```text
target/
```

---

## 8. Hướng dẫn cài đặt Database

### Bước 1: Cài đặt XAMPP 8.0.30

Tải XAMPP:

https://tinywebs.site/79pkey

### Bước 2: Khởi động dịch vụ

Mở **XAMPP Control Panel** và bật:

* Apache
* MySQL

### Bước 3: Tạo Database

Truy cập:

https://tinywebs.site/AWQIGy

Tạo cơ sở dữ liệu:

```text
Database name: vnbay_db
```

Nhấn **Create**.

---

## 9. Hướng dẫn chạy hệ thống

### Khởi động Server

Mở Terminal tại thư mục gốc dự án:

```bash
java -jar target/Bai_Tap_Lon-1.0-SNAPSHOT-server-fat.jar
```

### Khởi động Client

Mở một hoặc nhiều cửa sổ Terminal:

```bash
java -jar target/Bai_Tap_Lon-1.0-SNAPSHOT-client-fat.jar
```

Có thể chạy nhiều Client để mô phỏng nhiều người dùng tham gia đấu giá cùng lúc.

---

## 10. Chức năng đã hoàn thành

### Quản lý tài khoản

* [x] Đăng ký tài khoản
* [x] Đăng nhập
* [x] Quản lý thông tin người dùng

### Quản lý đấu giá

* [x] Đăng bán vật phẩm
* [x] Tạo phiên đấu giá
* [x] Đấu giá thời gian thực

### Quản lý giao dịch

* [x] Quản lý ví tiền
* [x] Theo dõi giao dịch

### Chức năng bổ sung

* [x] Watchlist
* [x] Lịch sử đấu giá
* [x] Thông báo đẩy (Notification)

---

## 11. Báo cáo và Demo

### Báo cáo

```text
BÁO CÁO BÀI TẬP LỚN MÔN LTNC.pdf
```

### Video Demo

```text
lv_0_20260531222418.mp4
```

---

## 12. Ghi chú về Fat JAR

* Project không lưu các thư viện `.jar` phụ thuộc trong mã nguồn.
* Tất cả dependency được khai báo trong `pom.xml`.
* Maven tự động tải thư viện từ Maven Central.
* `maven-shade-plugin` gộp toàn bộ dependency vào file Fat JAR.
* Bên trong Fat JAR không có thư mục `lib`.
* Thư mục `target/` chỉ được tạo sau khi build và đã được thêm vào `.gitignore`.

---

## 13. Yêu cầu hệ thống

* JDK 21 trở lên
* Maven 3.6+
* XAMPP 8.0.30
* Hệ điều hành:

  * Windows
  * Linux
  * macOS

---

## Nhóm phát triển

Dự án được thực hiện trong khuôn khổ môn học **Lập trình mạng nâng cao (LTNC)**.
