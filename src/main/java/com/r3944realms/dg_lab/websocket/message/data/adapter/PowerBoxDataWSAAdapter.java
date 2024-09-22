package com.r3944realms.dg_lab.websocket.message.data.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithSingleAttachment;

import java.io.IOException;

public class PowerBoxDataWSAAdapter extends PowerBoxDataAdapter{
    @Override
    public void write(JsonWriter out, PowerBoxData value) throws IOException {
        out.beginObject();
        PowerBoxDataWithSingleAttachment newValue = (PowerBoxDataWithSingleAttachment) value;
        out.name("type").value(newValue.getType());
        out.name("clientId").value(newValue.getClientId());
        out.name("targetId").value(newValue.getTargetId());
        out.name("message").value(newValue.getMessage());
        out.name("timer").value(newValue.getTimer());

        out.endObject();
    }

    @Override
    public PowerBoxData read(JsonReader in) throws IOException {
        String type = "";
        String clientId = "";
        String targetId = "";
        String message = "";
        Integer timer = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "type" -> type = in.nextString();
                case "clientId" -> clientId = in.nextString();
                case "targetId" -> targetId = in.nextString();
                case "message" -> message = in.nextString();
                case "timer" -> timer = in.nextInt();

            }
        }
        in.endObject();

        if ("POWER_BOX".equals(type)) {
            return new PowerBoxDataWithSingleAttachment(new PowerBoxData(type, clientId, targetId, message), timer);
        }
        // Handle other types
        throw new JsonParseException("Unknown type: " + type);
    }
}
