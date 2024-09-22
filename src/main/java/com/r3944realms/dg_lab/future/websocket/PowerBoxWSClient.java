package com.r3944realms.dg_lab.future.websocket;

import com.r3944realms.dg_lab.future.websocket.hanlder.ClientDLPBHandler;
import com.r3944realms.dg_lab.future.websocket.hanlder.ClientOperation;
import com.r3944realms.dg_lab.future.websocket.hanlder.ExampleOperation;
import com.r3944realms.dg_lab.future.websocket.sharedData.ClientPowerBoxSharedData;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class PowerBoxWSClient extends AbstractWebSocketClient {
    protected ClientPowerBoxSharedData sharedData;
    protected WebSocketClientRole role;
    protected ClientOperation operation;
    public PowerBoxWSClient(ClientPowerBoxSharedData sharedData, WebSocketClientRole role,ClientOperation operation) {
        this.sharedData = sharedData;
        this.operation = operation;
        this.role = role;
    }
    public PowerBoxWSClient(ClientPowerBoxSharedData sharedData, WebSocketClientRole role) {
        this(sharedData, role, new ExampleOperation());
    }
    @Override
    protected void MessagePipeLineHandler(ChannelPipeline pipeline) {
        pipeline.addLast(new ClientDLPBHandler(sharedData, role, operation));
    }

    @Override
    public void send(Message message) {
        if(message instanceof PowerBoxMessage PBMessage) {
            this.ClientChannel.writeAndFlush(new TextWebSocketFrame(PBMessage.getMsgJson()));
        } else {
            logger.error("Message is not a PowerBoxMessage");
        }
    }

}
