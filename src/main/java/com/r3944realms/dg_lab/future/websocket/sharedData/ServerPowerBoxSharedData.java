package com.r3944realms.dg_lab.future.websocket.sharedData;

import com.google.common.collect.Maps;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.Timer;

public class ServerPowerBoxSharedData implements ISharedData {

    public final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    // 映射表
    public final Map<String, String> channelIdMap = Maps.newConcurrentMap();
    // targetId -> PowerData
    public final Map<String, PowerBoxData> powerBoxDataMap = Maps.newConcurrentMap();

    // 储存已连接的用户及其标识
    public final Map<String, ChannelHandlerContext> connections = Maps.newConcurrentMap();
    // 存储消息关系
    public volatile Map<String, String> relations = Maps.newConcurrentMap();
    // 存储定时器
    public volatile Map<String, Timer> clientTimers = Maps.newConcurrentMap();
    //默认发送时间1秒
    public final int punishmentDuration;
    // 默认一秒发送1次
    public final int punishmentTime;
    // 心跳定时器（该为线程安全的类）
    public Timer heartTimer = null;

    public ServerPowerBoxSharedData() {
        this(5, 1);
    }
    public ServerPowerBoxSharedData(int punishmentDuration, int punishmentTime) {
        this.punishmentDuration = punishmentDuration > 0 ? punishmentDuration : 5;
        this.punishmentTime = punishmentTime> 0 ? punishmentTime : 1;
    }

}
