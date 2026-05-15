package com.example.bai_tap_lon.service;

import com.example.bai_tap_lon.model.User;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class UserSerializer implements JsonSerializer<User> {
    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("type", src.getRole().name());
        json.addProperty("id", src.getId());
        json.addProperty("username", src.getUsername());
        json.addProperty("password", src.getPassword());
        json.addProperty("fullName", src.getFullName());
        json.addProperty("locked", src.isLocked());
        return json;
    }
}
