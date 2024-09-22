package com.r3944realms.dg_lab.future.websocket;

import com.r3944realms.dg_lab.future.websocket.hanlder.ClientDLPBHandler;
import com.r3944realms.dg_lab.future.websocket.hanlder.ClientOperation;
import com.r3944realms.dg_lab.future.websocket.hanlder.ExampleOperation;
import com.r3944realms.dg_lab.future.websocket.sharedData.ClientPowerBoxSharedData;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;
import com.r3944realms.dg_lab.websocket.utils.RangeValidator;
import com.r3944realms.dg_lab.websocket.utils.stringUtils.UrlValidator;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PowerBoxWSClient extends AbstractWebSocketClient {
    protected ClientPowerBoxSharedData sharedData;
    protected WebSocketClientRole role;
    protected ClientOperation operation;
    public PowerBoxWSClient(ClientPowerBoxSharedData sharedData, WebSocketClientRole role,ClientOperation operation) {
        this.sharedData = sharedData;
        this.operation = operation;
        this.role = role;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            sharedData.address = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
        sharedData.port = 9000;
    }
    public PowerBoxWSClient(ClientPowerBoxSharedData sharedData, WebSocketClientRole role,ClientOperation operation, String address, int port) {
        super(address, port);
        this.sharedData = sharedData;
        this.operation = operation;
        this.role = role;
        sharedData.address = UrlValidator.isValidAddress(address) ? address : "";
        sharedData.port = RangeValidator.isValidPort(port) ? port : 9000;

    }
    public PowerBoxWSClient(ClientPowerBoxSharedData sharedData, WebSocketClientRole role) {
        this(sharedData, role, new ExampleOperation());
    }
    public PowerBoxWSClient(ClientPowerBoxSharedData sharedData, WebSocketClientRole role, String address, int port) {
        this(sharedData, role, new ExampleOperation(), address, port);
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
