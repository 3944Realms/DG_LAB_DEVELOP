package com.r3944realms.dg_lab.websocket.message.data.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithAttachment;

import java.io.IOException;
@Deprecated
public class PowerBoxDataWAAdapter extends PowerBoxDataAdapter {
    @Override
    public void write(JsonWriter out, PowerBoxData value) throws IOException {
        out.beginObject();
        PowerBoxDataWithAttachment newValue = (PowerBoxDataWithAttachment) value;
        out.name("type").value(newValue.getType());
        out.name("clientId").value(newValue.getClientId());
        out.name("targetId").value(newValue.getTargetId());
        out.name("message").value(newValue.getMessage());
        out.name("timer_A").value(newValue.getTimer_A());
        out.name("timer_B").value(newValue.getTimer_B());
        out.endObject();
    }

    @Override
    public PowerBoxData read(JsonReader in) throws IOException {
        String type = "";
        String clientId = "";
        String targetId = "";
        String message = "";
        Integer timer_A = null;
        Integer timer_B = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "type" -> type = in.nextString();
                case "clientId" -> clientId = in.nextString();
                case "targetId" -> targetId = in.nextString();
                case "message" -> message = in.nextString();
                case "timerA" -> timer_A = in.nextInt();
                case "timerB" -> timer_B = in.nextInt();
            }
        }
        in.endObject();

        if ("POWER_BOX".equals(type)) {
            return new PowerBoxDataWithAttachment(new PowerBoxData(type, clientId, targetId, message), timer_A, timer_B);
        }
        // Handle other types
        throw new JsonParseException("Unknown type: " + type);
    }
}
