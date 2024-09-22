package com.r3944realms.dg_lab.future.websocket;

import com.r3944realms.dg_lab.future.websocket.hanlder.ServerDLPBHandler;
import com.r3944realms.dg_lab.future.websocket.sharedData.ServerPowerBoxSharedData;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketServerRole;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class PowerBoxWSServer extends AbstractWebSocketServer {
    protected ServerPowerBoxSharedData SharedData;
    protected WebSocketServerRole role;
    public PowerBoxWSServer(ServerPowerBoxSharedData sharedData, WebSocketServerRole role) {
        this.SharedData = sharedData;
        this.role = role;
    }

    @Override
    protected void MessagePipeLineHandler(ChannelPipeline pipeline) {
        pipeline.addLast(new ServerDLPBHandler(SharedData, role));
    }

    @Override
    public void send(String connectId, Message message) {
        if(message instanceof PowerBoxMessage PBMessage) {
            ChannelHandlerContext context = SharedData.connections.get(connectId);//获取连接连接的Context
            if(context != null) {
                String targetId = SharedData.relations.get(connectId);
                if(targetId.isEmpty()) { //该上下文为客户端
                    context.writeAndFlush(new TextWebSocketFrame(PBMessage.getMsgJson()));//发送给客户端
                    ChannelHandlerContext appContext = SharedData.connections.get(targetId);
                    appContext.writeAndFlush(new TextWebSocketFrame(PBMessage.getDataJson()));//发送给APP端
                } else if(SharedData.relations.containsValue(targetId)){  // 绑定状态的APP端
                    //仅对APP发信息
                    context.writeAndFlush(new TextWebSocketFrame(PBMessage.getDataJson()));

                } else { // 未绑定状态（不可发送消息）
                    logger.error("Can't send Msg to no relationship obj.");
                }
            } else {
                logger.error("Find that Target is invalid.");
            }
        } else {
            logger.error("Message is not a PowerBoxMessage");
        }
    }
}
