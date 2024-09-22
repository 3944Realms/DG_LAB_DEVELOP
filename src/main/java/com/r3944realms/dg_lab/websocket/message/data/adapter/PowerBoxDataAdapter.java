package com.r3944realms.dg_lab.websocket.message.data.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;

import java.io.IOException;

public class PowerBoxDataAdapter extends TypeAdapter<PowerBoxData> {
        @Override
        public void write(JsonWriter out, PowerBoxData value) throws IOException {
            out.beginObject();
            out.name("type").value(value.getType());
            out.name("clientId").value(value.getClientId());
            out.name("targetId").value(value.getTargetId());
            out.name("message").value(value.getMessage());
            out.endObject();
        }

        @Override
        public PowerBoxData read(JsonReader in) throws IOException {
            String type = "";
            String clientId = "";
            String targetId = "";
            String message = "";

            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "type":
                        type = in.nextString();
                        break;
                    case "clientId":
                        clientId = in.nextString();
                        break;
                    case "targetId":
                        targetId = in.nextString();
                        break;
                    case "message":
                        message = in.nextString();
                        break;
                }
            }
            in.endObject();

            if ("POWER_BOX".equals(type)) {
                return new PowerBoxData(type, clientId, targetId, message);
            }
            // Handle other types
            throw new JsonParseException("Unknown type: " + type);
        }
}


