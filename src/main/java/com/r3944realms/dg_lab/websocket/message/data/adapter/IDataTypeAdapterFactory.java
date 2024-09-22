package com.r3944realms.dg_lab.websocket.message.data.adapter;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.r3944realms.dg_lab.websocket.message.data.IData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithAttachment;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithSingleAttachment;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class IDataTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        if(!IData.class.isAssignableFrom(typeToken.getRawType())) {
            return null;
        }
        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
        final TypeAdapter<PowerBoxData> powerBoxDataAdapter = gson.getDelegateAdapter(this, TypeToken.get(PowerBoxData.class));
        final TypeAdapter<PowerBoxDataWithAttachment> powerBoxDataWithAttachmentAdapter = gson.getDelegateAdapter(this, TypeToken.get(PowerBoxDataWithAttachment.class));
        final TypeAdapter<PowerBoxDataWithSingleAttachment> powerBoxDataWithSingleAttachmentTypeAdapter = gson.getDelegateAdapter(this, TypeToken.get(PowerBoxDataWithSingleAttachment.class));
        return (TypeAdapter<T>) new TypeAdapter<IData>() {
            @Override
            public void write(JsonWriter jsonWriter, IData iData) throws IOException {
                JsonElement jsonElement = switch (iData) {
                    case PowerBoxDataWithAttachment dataWithAttachment ->
                            powerBoxDataWithAttachmentAdapter.toJsonTree(dataWithAttachment);
                    case PowerBoxDataWithSingleAttachment powerBoxDataWithSingleAttachment ->
                            powerBoxDataWithSingleAttachmentTypeAdapter.toJsonTree(powerBoxDataWithSingleAttachment);
                    case PowerBoxData powerBoxData -> powerBoxDataAdapter.toJsonTree(powerBoxData);
                    case null ->
                        throw new NullPointerException("IDataTypeAdapterFactory#create(Gson gson, TypeToken iData): null");
                    default ->
                            throw new JsonSyntaxException("Unsupported data type: " + iData.getClass().getName());
                };
                elementAdapter.write(jsonWriter, jsonElement);
            }

            @Override
            public IData read(JsonReader jsonReader) throws IOException {
                JsonElement element = elementAdapter.read(jsonReader);
                JsonObject jsonObject = element.getAsJsonObject();

                String Eigenvalues_A = jsonObject.get("type").getAsString();
                switch (Eigenvalues_A) {
                    case "heartbeat", "error", "msg", "break", "bind" -> {
                        return powerBoxDataAdapter.fromJsonTree(element);
                    }
                    case "clientMsg" -> {
                        return powerBoxDataWithAttachmentAdapter.fromJsonTree(element);
                    }
                    default -> throw new JsonParseException("Unknown type");

                }
            }
        };

    }
}
