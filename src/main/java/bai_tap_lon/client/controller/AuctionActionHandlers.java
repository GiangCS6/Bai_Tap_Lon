package bai_tap_lon.client.controller;

import java.util.function.Consumer;

/**
 * Bundle các callback mà Strategy cần để gắn vào button.
 *
 * Controller (Context) implement các handler thật: handlePlaceBid, handleWatch,
 * handleCancel, openDetail rồi truyền vào Strategy. Strategy chỉ quyết định
 * "role này hiển thị nút nào, gắn handler nào" — không tự gọi network.
 *
 * Dùng Consumer<String> vì tất cả handler đều nhận auctionId làm tham số.
 * Field nào không cần (vd: seller không dùng placeBid) thì truyền null hoặc
 * no-op — Strategy tự bỏ qua.
 */
public final class AuctionActionHandlers {

    public final Consumer<String> onPlaceBid;
    public final Consumer<String> onWatch;
    public final Consumer<String> onCancel;
    public final Consumer<String> onOpenDetail;

    private AuctionActionHandlers(Builder b) {
        this.onPlaceBid   = nullSafe(b.onPlaceBid);
        this.onWatch      = nullSafe(b.onWatch);
        this.onCancel     = nullSafe(b.onCancel);
        this.onOpenDetail = nullSafe(b.onOpenDetail);
    }

    private static Consumer<String> nullSafe(Consumer<String> c) {
        return c == null ? id -> {} : c;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Consumer<String> onPlaceBid;
        private Consumer<String> onWatch;
        private Consumer<String> onCancel;
        private Consumer<String> onOpenDetail;

        public Builder onPlaceBid(Consumer<String> h)   { this.onPlaceBid = h; return this; }
        public Builder onWatch(Consumer<String> h)      { this.onWatch = h; return this; }
        public Builder onCancel(Consumer<String> h)     { this.onCancel = h; return this; }
        public Builder onOpenDetail(Consumer<String> h) { this.onOpenDetail = h; return this; }

        public AuctionActionHandlers build() { return new AuctionActionHandlers(this); }
    }
}
