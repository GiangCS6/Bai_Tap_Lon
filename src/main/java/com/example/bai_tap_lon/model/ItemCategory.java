package com.example.bai_tap_lon.model;

/**
 * [KIẾN THỨC BỔ SUNG: Enum]
 * Giải thích: Enum (Enumeration) là một kiểu dữ liệu đặc biệt trong Java dùng để định nghĩa
 * một tập hợp các hằng số (constants).
 * Ưu điểm: Đảm bảo tính toàn vẹn dữ liệu. Khi gán danh mục cho Item, bạn chỉ có thể chọn
 * 1 trong 3 giá trị này, ngăn chặn việc nhập sai tên danh mục (vd: nhập "Electronic" thay vì "ELECTRONICS").
 */
public enum ItemCategory {
    ELECTRONICS,
    ART,
    VEHICLE
}
