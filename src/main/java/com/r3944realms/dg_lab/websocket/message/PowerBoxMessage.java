package com.r3944realms.dg_lab.websocket.message;

import com.google.gson.JsonSyntaxException;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithAttachment;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithSingleAttachment;
import com.r3944realms.dg_lab.websocket.message.data.type.PowerBoxDataType;
import com.r3944realms.dg_lab.websocket.message.role.PlaceholderRole;
import com.r3944realms.dg_lab.websocket.message.role.Role;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketServerRole;

public class PowerBoxMessage extends Message {

    public PowerBoxDataType commandType;
    private static final PowerBoxMessage Null = PowerBoxMessage.createPowerBoxMessage("null","null","null","null", new PlaceholderRole("null"), new PlaceholderRole("null"));
    static String INVALID_MESSAGE_JSON = gson.toJson(PowerBoxData.createPowerBoxData("error","","",""));
    public PowerBoxMessage(PowerBoxData payload, MessageDirection<?, ?> direction) {
        super(payload ,direction);
        commandType = payload.getCommandType();
    }
    public static PowerBoxMessage createPowerBoxMessage(
            String type, String clientId, String targetId, String message,
            Role sender, Role receiver
    ) {
        PowerBoxData data = PowerBoxData.createPowerBoxData(type, clientId, targetId, message);
        MessageDirection<?,?> direction = new MessageDirection<>(
                sender,
                receiver
        );
        return new PowerBoxMessage(data, direction);
    }

    /**
     * 仅供转化用，不可使用与传递
     * @return Null_Message
     */
    public static PowerBoxMessage getNullMessage() {
        return Null;
    }
    @Override
    public String AdditionalInformation() {
        return "IPowerBoxMessage : " + direction.toString();
    }

    @Override
    public String getInvalidMessageJson() {
        return INVALID_MESSAGE_JSON;
    }

    @Override
    public PowerBoxMessage readJsonReturnMessage(String dataJson, MessageDirection<?, ?> messageDirection) throws JsonSyntaxException {
        return new PowerBoxMessage(getPayload(dataJson), messageDirection);
    }
    /**
     *
     * @param json PowerBoxDataJSON
     * @return PowerBoxData （如果Data字段存在空则会返回null值）
     */
    @Override
    public PowerBoxData getPayload(String json) throws JsonSyntaxException {
        PowerBoxData powerBoxData = gson.fromJson(json, PowerBoxData.class);
        return (powerBoxData.Type() != null && powerBoxData.getClientId() != null && powerBoxData.getTargetId() != null && powerBoxData.getMessage() != null) ? powerBoxData : null;
    }

    /**
     *
     * @param json PowerBoxMessageJSON
     * @return PowerBoxMessage （如果message字段存在空则会返回null值）
     */
    @Override
    public PowerBoxMessage getMessage(String json) {
        PowerBoxMessage message = gson.fromJson(json, PowerBoxMessage.class);
        return (message.direction != null && message.payload != null && message.commandType != null) ? message : null;
    }

    public PowerBoxDataWithAttachment getPayloadWithAttachment(String json) throws JsonSyntaxException {
        return gson.fromJson(json, PowerBoxDataWithAttachment.class);
    }
    public PowerBoxDataWithSingleAttachment getPayloadWithSingleAttachment(String json) throws JsonSyntaxException {
        return gson.fromJson(json, PowerBoxDataWithSingleAttachment.class);
    }

    //json数据的字符最大长度为1950，若超过该长度，APP收到数据将会丢弃该消息 842 (最大100组波形波频数据) - 64 ，足矣
    public static void main(String[] args) {
        PowerBoxData data = PowerBoxData.createPowerBoxData("msg", "xxxx-xxxxxxxxx-xxxxx-xxxxx-xx", "xxxx-xxxxxxxxx-xxxxx-xxxxx-xx", "pulse-A:"+ PowerBoxData.reformWaveDataList(new String[]{"ff00ff00","00ff00ff","15002233"}));
        PowerBoxDataWithAttachment dataWithAttachment = PowerBoxDataWithAttachment.attach(data, 0, 0);
        MessageDirection<WebSocketClientRole, WebSocketServerRole> mD = new MessageDirection<>(new WebSocketClientRole("uuid"), new WebSocketServerRole("uuid"));
        PowerBoxMessage msg = new PowerBoxMessage(dataWithAttachment, mD),ms = null;
        String message = msg.getMsgJson();
        PowerBoxData powerBoxData = null;
        String msgData = msg.getDataJson();
        try {
            ms = gson.fromJson(msgData, PowerBoxMessage.class);
            if(ms.payload == null || ms.direction == null || ms.commandType == null) throw new NullPointerException("invalid message");
        }catch(Exception e) {
            e.printStackTrace();
            ms = null;
            powerBoxData = gson.fromJson(msgData ,PowerBoxData.class);
        }
        if(ms == null) {
            System.out.println(null != powerBoxData ? powerBoxData.getType() : "No PowerBoxData");
        } else {
            System.out.println(ms.getMsgJson());
        }
//        System.out.println(message);
//        PowerBoxMessage msg_formJSON = gson.fromJson(message, PowerBoxMessage.class);
//        System.out.println(msg + "\n" + msg_formJSON);
//        System.out.println(msg_formJSON.getMsgJson());
//        System.out.println(msg.AdditionalInformation());
//        System.out.println(msg.getDataJson());
    }
}
