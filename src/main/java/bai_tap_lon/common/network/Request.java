package bai_tap_lon.common.network;
import com.google.gson.JsonObject;

public class Request {

    private final String action;
    private final JsonObject payload;

    //Constructor private
    private Request(Builder builder) {
        this.action = builder.action;
        this.payload = builder.payload;
    }

    // getter
    public String getAction() { return action; }
    public JsonObject getPayload() { return payload; }

    // Builder
    public static class Builder {
        private String action;
        private JsonObject payload;

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder payload(JsonObject payload) {
            this.payload = payload;
            return this;
        }

        public Request build() {
            //kiem tra logic
            if (this.action == null || this.action.trim().isEmpty()) {
                throw new IllegalArgumentException("Action không được để trống!");
            }
            return new Request(this);
        }
    }
}
