package bai_tap_lon.common.exception;

import com.google.gson.JsonObject;

public final class BusinessException extends RuntimeException {
    final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code    = code;
    }

    public String getCode(){
        return code;
    }
}
