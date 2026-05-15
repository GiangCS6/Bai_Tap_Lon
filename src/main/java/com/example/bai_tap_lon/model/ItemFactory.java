package com.example.bai_tap_lon.model;

/**
 * [KIẾN THỨC BỔ SUNG: Factory Method Pattern]
 * Giải thích: Đây là một Creational Design Pattern. Thay vì gọi trực tiếp từ khóa 'new' (vd: new Electronics(...))
 * rải rác khắp nơi trong code (ví dụ: ở UI, ở file xử lý logic), ta gom logic tạo đối tượng vào một nơi duy nhất là Factory.
 * Ưu điểm:
 * 1. Giấu đi sự phức tạp khi khởi tạo object.
 * 2. Dễ dàng mở rộng. Sau này thêm loại 'Bất động sản', chỉ cần sửa trong Factory, không ảnh hưởng đến các phần code khác.
 */
public class ItemFactory {

    // Phương thức tạo Item dựa trên Category
    public static Item createItem(ItemCategory category, String name, String desc, double price, String sellerId, Object extraData) {
        switch (category) {
            case ELECTRONICS:
                // Ép kiểu extraData (ví dụ: số tháng bảo hành)
                int warranty = (Integer) extraData;
                return new Electronics(name, desc, price, sellerId, warranty);

            case ART:
                String artist = (String) extraData;
                return new Art(name, desc, price, sellerId, artist);

            case VEHICLE:
                int year = (Integer) extraData;
                return new Vehicle(name, desc, price, sellerId, year);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ!");
        }
    }
}
