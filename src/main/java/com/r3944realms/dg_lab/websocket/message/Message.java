package com.r3944realms.dg_lab.websocket.message;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.r3944realms.dg_lab.websocket.message.data.IData;
import com.r3944realms.dg_lab.websocket.message.data.adapter.IDataTypeAdapterFactory;
import com.r3944realms.dg_lab.websocket.message.role.Role;
import com.r3944realms.dg_lab.websocket.message.role.deserializer.RoleDeserializer;

import java.io.Serial;
import java.io.Serializable;


public abstract class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Expose(deserialize = false, serialize = false)
    final static Gson gson;
    final public MessageDirection<?,?> direction;
    final IData payload;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Role.class, new RoleDeserializer());
        gsonBuilder.registerTypeAdapter(MessageDirection.class, new MessageDirectionDeserializer());
        gsonBuilder.registerTypeAdapterFactory(new IDataTypeAdapterFactory());
        gson = gsonBuilder.create();
    }
    /**
     * 额外的信息, 如
     * <li>消息发送者UUID</li>
     * <li>消息创建时间</li>
     * <li>信息校对值</li>
     * <li>...</li>
     * @return 信息
     */
    public abstract String AdditionalInformation();

    Message(IData payload, MessageDirection<?, ?> direction) {
        this.payload = payload;
        this.direction = direction;
    }
    public String getDataJson() {
        return getDataJson(false);
    }

    /**
     * 无效信息返回
     * @return Json
     */
    public abstract String getInvalidMessageJson();
    public String getDataJson(boolean isFix) {
        if(payload == null) return getInvalidMessageJson();
        return payload.isValid() ? (isFix ? gson.toJson(payload).replace("\\","") : gson.toJson(payload)) : getInvalidMessageJson();
    }
    public String getMsgJson() {
        return getMsgJson(false);
    }
    public String getMsgJson(boolean isFix) {
        if(payload == null && direction == null) return getInvalidMessageJson();
        return isFix ? gson.toJson(this).replace("\\","") : gson.toJson(this);
    }

    public abstract Message readJsonReturnMessage(String dataJson, MessageDirection<?, ?> messageDirection);
    public abstract IData getPayload(String json);
    public abstract Message getMessage(String json);
}
