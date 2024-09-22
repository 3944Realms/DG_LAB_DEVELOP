package com.r3944realms.dg_lab.websocket.timeTask;


import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.role.PlaceholderRole;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;
import com.r3944realms.dg_lab.websocket.utils.annoation.NeedCompletedInFuture;
import io.netty.channel.ChannelHandlerContext;

import java.util.TimerTask;
import java.util.function.Consumer;

public class DgLabTimerTask extends TimerTask {
    ChannelHandlerContext client, target;
    String clientId, targetId;
    Integer totalSends;
    Message sendMessageData;
    Consumer<Object> timerConsumer;
    char channel;
    public DgLabTimerTask(ChannelHandlerContext client, ChannelHandlerContext target, Message sendMessageData, Integer totalSends, Consumer<Object> timerConsumer, char channel) {
        this.channel = channel;
        this.client = client;
        this.target = target;
        this.sendMessageData = sendMessageData;
        this.totalSends = totalSends;
        this.timerConsumer = timerConsumer;
    }

    public void setId(String clientId, String targetId) {
        this.clientId = clientId;
        this.targetId = targetId;
    }

    @Override
    public void run() {
        if(totalSends > 0) {//一一个固定频率发送特定的客户端
            send(target,sendMessageData);
            totalSends--;
        }
        if(totalSends <= 0) {//达到发送上限
            PowerBoxMessage over = PowerBoxMessage.createPowerBoxMessage("clientMsg", clientId, targetId, "send-over", new PlaceholderRole("Server"), new WebSocketClientRole("Cl" + clientId));
            send(client, over);
            timerConsumer.accept(null);
        }
    }
    @NeedCompletedInFuture
    private void send(ChannelHandlerContext target, Message message) {
        target.writeAndFlush(message.getDataJson());
    }
}
