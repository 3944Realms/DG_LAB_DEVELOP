package com.r3944realms.dg_lab.websocket.message.role.deserializer;

import com.google.gson.*;
import com.r3944realms.dg_lab.websocket.message.role.*;
import com.r3944realms.dg_lab.websocket.message.role.type.RoleType;

import java.lang.reflect.Type;

public class RoleDeserializer implements JsonDeserializer<Role> {

    @Override
    public Role deserialize(JsonElement json, Type typeOFT, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String name = jsonObject.get("name").getAsString();
        RoleType type = RoleType.getTypeFromString(jsonObject.get("type").getAsString());
        if (type != null) {
            return switch (type){
                case T_CLIENT -> new WebSocketClientRole(name);
                case T_SERVER -> new WebSocketServerRole(name);
                case APPLICATION -> new WebSocketApplicationRole(name);
                case PLACEHOLDER -> new PlaceholderRole(name);
            };
        }
        return null;
    }
}
