package bai_tap_lon.common.network;

import com.google.gson.JsonObject;

public class Response {

    private final String action;
    private final boolean success;
    private final JsonObject data;
    private final String error;
    private final String errorMessage;

    //constructor private
    private Response(Builder builder) {
        this.action = builder.action;
        this.success = builder.success;
        this.data = builder.data;
        this.error = builder.error;
        this.errorMessage = builder.errorMessage;
    }

    // getter
    public String getAction() { return action; }
    public boolean isSuccess() { return success; }
    public JsonObject getData() { return data;}
    public String getError() { return error; }
    public String getErrorMessage(){
        return errorMessage;
    }

    // Class Builder
    public static class Builder {
        private String action;
        private boolean success;
        private JsonObject data;
        private String error;
        private String errorMessage;

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder data(JsonObject data) {
            this.data = data;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }
}
