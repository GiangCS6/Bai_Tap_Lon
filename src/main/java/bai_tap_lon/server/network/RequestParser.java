package bai_tap_lon.server.network;

import bai_tap_lon.common.network.Request;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class RequestParser {
    private static final Gson gson = new Gson();
    public static Request parseRequest(String json){
        try{
            JsonObject parse = gson.fromJson(json,JsonObject.class);
            //
            if (parse == null) {
                throw new IllegalArgumentException("Request JSON là null");
            }
            // missing action field hoặc là null
            if(!parse.has("action") || parse.get("action").isJsonNull()){
                throw new IllegalArgumentException("Thiếu field bắt buộc: action");
            }
            //action empty
            String action = parse.get("action").getAsString();
            if (action.isBlank()) {
                throw new IllegalArgumentException("action không được để trống");
            }
            //payload empty
            if (!parse.has("payload")){
                throw new IllegalArgumentException("Thiếu field bắt buộc: payload");
            }
            JsonObject payload = parse.get("payload").getAsJsonObject();
            return new Request.Builder()
                    .action(action)
                    .payload(payload)
                    .build();
        }
        catch (JsonSyntaxException | IllegalStateException e) {
            throw new IllegalArgumentException("JSON không hợp lệ: " + e.getMessage(), e);
        }

    }
}
