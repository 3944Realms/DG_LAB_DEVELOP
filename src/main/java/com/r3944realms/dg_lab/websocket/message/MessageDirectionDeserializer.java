package com.r3944realms.dg_lab.websocket.message;

import com.google.gson.*;
import com.r3944realms.dg_lab.websocket.message.role.Role;

import java.lang.reflect.Type;

public class MessageDirectionDeserializer implements JsonDeserializer<MessageDirection<?, ?>> {
    @Override
    public MessageDirection<?, ?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        JsonElement senderJson = obj.get("sender");
        JsonElement receiverJson = obj.get("receiver");

        Role sender = jsonDeserializationContext.deserialize(senderJson, Role.class);
        Role receiver = jsonDeserializationContext.deserialize(receiverJson, Role.class);
        return new MessageDirection<>(sender, receiver);
    }
}
