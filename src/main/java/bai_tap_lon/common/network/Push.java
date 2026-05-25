package bai_tap_lon.common.network;
import com.google.gson.JsonObject;

public class Push {
    private final boolean isPushEvent = true; // used by client to distinguish push from response
    private final String action;
    private final JsonObject data;

    private Push(Builder builder) {
        this.action = builder.action;
        this.data = builder.data;
    }

    public JsonObject getData(){
        return data;
    }
    public String getAction() {
        return action;
    }

    public static class Builder{
        private String action;
        private JsonObject data;

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder data(JsonObject data) {
            this.data = data;
            return this;
        }

        public Push build() {
            return new Push(this);
        }
    }

    public static Push build(String action, JsonObject data){
        return new Builder().action(action).data(data).build();
    }
}