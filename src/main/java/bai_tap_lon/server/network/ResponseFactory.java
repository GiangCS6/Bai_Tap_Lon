package bai_tap_lon.server.network;

import bai_tap_lon.common.network.Response;
import com.google.gson.JsonObject;

public class ResponseFactory {


    /// Create error Response with message and error code
    public static Response error (String action, String errorCode,String errorMessage){
        return new Response.Builder()
                .action(action)
                .data(null)
                .success(false)
                .error(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    /// ____OK Response with data____
    public static Response ok(String action, JsonObject jsonObject){
        return new Response.Builder()
                .action(action)
                .data(jsonObject)
                .success(true)
                .error(null)
                .errorMessage(null)
                .build();
    }

    /// Overload OK Response with no data
    public static Response ok (String action){
        return new Response.Builder()
                .action(action)
                .data(null)
                .success(true)
                .error(null)
                .errorMessage(null)
                .build();
    }
}
