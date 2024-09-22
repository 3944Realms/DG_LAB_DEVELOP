package com.r3944realms.dg_lab.future.websocket.hanlder;

import com.r3944realms.dg_lab.future.websocket.sharedData.ServerPowerBoxSharedData;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.message.MessageDirection;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithSingleAttachment;
import com.r3944realms.dg_lab.websocket.message.data.type.PowerBoxDataType;
import com.r3944realms.dg_lab.websocket.message.role.*;
import com.r3944realms.dg_lab.websocket.message.role.type.RoleType;
import com.r3944realms.dg_lab.websocket.protocol.ServerMessageTextWebsocketHandler;
import com.r3944realms.dg_lab.websocket.timeTask.DgLabTimerTask;
import com.r3944realms.dg_lab.websocket.utils.stringUtils.StringHandlerUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;



public class ServerDLPBHandler extends AbstractDgLabPowerBoxHandler implements IAttachSharedData{

    private final ServerPowerBoxSharedData SharedData;
    public final WebSocketServerRole role;
    public ServerDLPBHandler(ServerPowerBoxSharedData serverPowerBoxSharedData, WebSocketServerRole role) {
        this.SharedData = serverPowerBoxSharedData;
        this.role = role;
    }

    public ServerPowerBoxSharedData getSharedData() {
        return SharedData;
    }


    @Override
    public void SessionBuildInHandle(ChannelHandlerContext session) {
        String clientId;
        do{
             clientId =  UUID.randomUUID().toString();
        } while (ChannelIdMap().containsKey(clientId));
        ChannelIdMap().put(session.channel().id().asLongText(), clientId);
        logger.debug("channel added: clientId={}", clientId);
        Channel().add(session.channel());
        Connections().put(clientId, session);
        logger.info("新的 webSocket 连接已建立, 标识符为{}", clientId);
    }

    @Override
    public void ActiveSessionHandle(ChannelHandlerContext session) {
        String clientId = ChannelIdMap().get(session.channel().id().asLongText());
        PowerBoxMessage bindMsg = PowerBoxMessage.createPowerBoxMessage("bind", clientId, "", "targetId",
                role, new PlaceholderRole("Pl" + clientId)
        );
        // 延迟发送消息
        session.executor().schedule(() -> {
            if (session.channel().isActive() && session.channel().isOpen()) {
                session.channel().writeAndFlush(new TextWebSocketFrame(bindMsg.getDataJson())).addListener(sendFuture -> {
                    if (sendFuture.isSuccess()) {
                        logger.info("#1 Message sent successfully to clientId={}", clientId);
                    } else {
                        logger.error("#1 Failed to send message to clientId={}", clientId, sendFuture.cause());
                    }
                });
            } else {
                logger.debug("Channel is not active, message not sent to clientId={}", clientId);
            }
        }, 200, TimeUnit.MILLISECONDS); // 延迟200毫秒发送消息
        if(HeartTimer() != null) return;
        synchronized (ServerMessageTextWebsocketHandler.class) {
            // 启动心跳定时器（如果尚未启动）
            if (HeartTimer() == null) {
                SharedData.heartTimer = new Timer();
                HeartTimer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!Connections().isEmpty()) {
                            logger.debug("关系池大小：{}连接池大小：{},发送心跳消息", Relations().size(), Connections().size());
                        }
                        for (String clientId : Connections().keySet()) {
                            ChannelHandlerContext client = Connections().get(clientId);
                            if(client != null && client.channel().isActive() && client.channel().isOpen()) {
                                String targetId = Relations().get(clientId);
                                if (ObjectUtils.isEmpty(targetId)) {
                                    targetId = "";
                                }
                                Role receiverRole = getRole(clientId);
                                PowerBoxMessage message =
                                        PowerBoxMessage.createPowerBoxMessage("heartbeat", clientId, targetId, "200",
                                                role, receiverRole);
                                sendMessageOrData(ServerDLPBHandler.this, clientId, message);
                            } else {
                                logger.debug("Channel is not active, skipping heartbeat for clientId={}", clientId);
                                Connections().remove(clientId);//不活跃移除对应连接
                            }
                        }
                    }
                }, 500, 60000/*ms = 1min*/);
            }
        }
    }

    private @NotNull Role getRole(String clientId) {
        RoleType roleType = getRoleType(this, clientId);
        Role receiverRole;
        switch (roleType) {
            case T_CLIENT -> receiverRole = new WebSocketClientRole("Cl" + clientId);
            case APPLICATION -> receiverRole = new WebSocketApplicationRole("Ap" + clientId);
            default -> receiverRole = new PlaceholderRole("Pl" + clientId);
        }
        return receiverRole;
    }

    @Override
    public void SessionCloseHandle(ChannelHandlerContext session) {
        logger.debug("Websocket 连接已关闭");
        String channelId = session.channel().id().asLongText();
        String disconnectId = ChannelIdMap().remove(channelId);
        logger.debug("channel removed: channelId={}", channelId);
        logger.info("断开的client Id:{}", disconnectId);
        Channel().remove(session.channel());
        for(String _clientId_ : Relations().keySet()) {
            String _targetId_ = Relations().get(_clientId_);
            if(_clientId_.equals(disconnectId)) {
                //Client断开 ，通知app
                ChannelHandlerContext appClient = Connections().get(_targetId_);
                PowerBoxMessage message =
                        PowerBoxMessage.createPowerBoxMessage("break", disconnectId, _targetId_, "209", role, new WebSocketClientRole("Cl" + _targetId_));
                sendMessageText(this, appClient, message);
                appClient.close(); //关闭当前的 websocket 连接
                Relations().remove(_clientId_); // 清除关系
                PowerBoxDataMap().remove(_targetId_);
                logger.debug("Close Application Connecting{}", _targetId_);
                Connections().remove(_targetId_);//并移除targetId
            } else if(_targetId_.equals(disconnectId)) {
                //app断开,通知Client
                ChannelHandlerContext client = Connections().get(_clientId_);
                PowerBoxMessage message =
                        PowerBoxMessage.createPowerBoxMessage("break", _clientId_, _targetId_, "209", role, new WebSocketApplicationRole("Ap" + _targetId_));
                sendMessageData(this, client, message);
                client.close();//关闭当前的 websocket 连接
                Relations().remove(_clientId_);// 清除关系
                PowerBoxDataMap().remove(_targetId_);
                logger.debug("Close Client Connecting{}", _targetId_);
                Connections().remove(_clientId_);//并移除clientId
            }
            Connections().remove(disconnectId);//移除断开连接的对方
            Channel().remove(session.channel());
            logger.debug("channel removed: channelId={}[Current Size:{}]", channelId, Connections().size());
        }
    }

    @Override
    public void ReadMsgHandle(ChannelHandlerContext session, TextWebSocketFrame msg) {
        logger.debug("收到消息：{}", msg);
        PowerBoxMessage pMessage;
        PowerBoxData data;
        try {
            data = PowerBoxMessage.getNullMessage().getPayload(msg.text());
            MessageDirection<PlaceholderRole, WebSocketServerRole> direction = new MessageDirection<>(
                    new PlaceholderRole("Pl" + data.getClientId()), role
            );
            pMessage = new PowerBoxMessage(data, direction);
        } catch (Exception e) { //非JSON消息
            pMessage = PowerBoxMessage.createPowerBoxMessage("error", "", "",  "403",
                    role, new WebSocketClientRole("ErrorReceiver"));
            sendMessageText(this, session, pMessage);
            return;
        }
        if(!Connections().containsKey(data.getClientId()) && !Connections().containsKey(data.getTargetId())) { //非法信息来源拒绝
            pMessage = PowerBoxMessage.createPowerBoxMessage("error", "", "", "404",
                    role, new WebSocketClientRole("ErrorReceiver"));
            sendMessageText(this, session, pMessage);
            return;
        }
        if(!ObjectUtils.isEmpty(data.getType()) && !ObjectUtils.isEmpty(data.getClientId()) && !ObjectUtils.isEmpty(data.getTargetId()) && !ObjectUtils.isEmpty(data.getMessage())) {
            String type = data.getType();
            String clientId = data.getClientId();
            String targetId = data.getTargetId();
            String message = data.getMessage();
            PowerBoxDataType dataType = PowerBoxDataType.getType(type, message);
            switch (dataType) {
                case _NC_BIND_ -> {
                    //服务器下发绑定关系
                    if (Connections().containsKey(clientId) && Connections().containsKey(targetId)){
                        //relations的双方都不存在这对id 且 信息为DGLAB
                        if(!(Relations().containsKey(clientId) || Relations().containsKey(targetId) || Relations().containsValue(clientId) || Relations().containsValue(targetId)) && message.equals("DGLAB")) {
                            Relations().put(clientId, targetId);
                            ChannelHandlerContext client = Connections().get(clientId);
                            PowerBoxMessage bindMessage = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "200", role, new PlaceholderRole("Both: " + clientId + " ^ " + targetId));
                            sendMessageData(this, session, bindMessage);
                            sendMessageText(this, client, bindMessage);
                        } else {
                            PowerBoxMessage failure = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "400", role, new WebSocketApplicationRole("Ap" + targetId));
                            sendMessageData(this, session, failure);
                        }
                    } else {
                        PowerBoxMessage failure = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "401", role, new WebSocketApplicationRole("Ap" + targetId));
                        sendMessageData(this, session, failure);
                    }
                }
                case STRENGTH,PULSE,CLEAR,FEEDBACK -> {
                    if(Relations().containsKey(clientId) && !Relations().get(clientId).equals(targetId)) {//没有绑定关系
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "402", role, new WebSocketApplicationRole("Ap" + targetId));
                        sendMessageData(this, session, error);
                        return;
                    }
                    Object[] argsArray = data.getArgsArray();
                    if(argsArray == null) {
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, "500", role, new WebSocketApplicationRole("Ap" + targetId));
                        sendMessageData(this, session, error);
                        return;
                    }
                    switch (dataType) {
                        case STRENGTH ->{
                            if(Connections().containsKey(targetId)) {
                                ChannelHandlerContext app = Connections().get(targetId);
                                ChannelHandlerContext client = Connections().get(clientId);
                                if(argsArray.length == 3){
                                    int channel = ((Integer[])argsArray)[0];
                                    int policyChange = ((Integer[])argsArray)[1];
                                    int strengthChangeValue = ((Integer[])argsArray)[2];
                                    String messageCommand = "strength-" + channel + "+" + policyChange + "+" + strengthChangeValue;
                                    PowerBoxMessage strengthUpdate = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, messageCommand, role, new WebSocketApplicationRole("Ap" + targetId));
                                    sendMessageData(this, app, strengthUpdate);
                                } else if(argsArray.length == 4){
                                    int AStrength = ((Integer[])argsArray)[0];
                                    int BStrength = ((Integer[])argsArray)[1];
                                    int ALimit = ((Integer[])argsArray)[2];
                                    int BLimit = ((Integer[])argsArray)[3];
                                    putDataInMap(targetId, data);
                                    String currentStrengthMsg = "strength-" + AStrength + "+" + BStrength + "+" + ALimit + "+" + BLimit;
                                    PowerBoxMessage clientMsg = PowerBoxMessage.createPowerBoxMessage("clientMsg", clientId, targetId, currentStrengthMsg, role, new WebSocketClientRole("Cl" + clientId));
                                    sendMessageText(this, client, clientMsg);
                                }
                            }
                        }
                        case PULSE -> {
                            //Thrown UnsupportedMsg
                            PowerBoxMessage UnsupportedMsg = PowerBoxMessage.createPowerBoxMessage("error", clientId, targetId, "502", role, new WebSocketClientRole("Cl" + clientId));
                            //"unsupported-reason:(please use type named clientMsg to adjust wave in order to get the timers of AB)"
                            sendMessageText(this ,session, UnsupportedMsg);//发送给客户端
                        }
                        case CLEAR -> {
                            if(Connections().containsKey(targetId)) {
                                ChannelHandlerContext client = Connections().get(targetId);
                                String Channel = ((String[])argsArray)[0];
                                String messageCommand = "clear-" + Channel;
                                PowerBoxMessage strengthUpdate = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, messageCommand, role, new WebSocketApplicationRole("Ap" + targetId));
                                sendMessageData(this, client, strengthUpdate);
                            }
                        }
                        case FEEDBACK -> {
                            if(Connections().containsKey(targetId)) {
                                ChannelHandlerContext app = Connections().get(targetId);
                                int index = ((Integer[])argsArray)[0];
                                String feedBack = "feedback-" + index;
                                PowerBoxMessage feedBackMsg = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, feedBack, role, new WebSocketApplicationRole("Ap" + targetId));
                                sendMessageData(this, app, feedBackMsg);
                            }
                        }
                    }

                }
                case CLIENT_MESSAGE -> {//接收到客户端消息(客户端的作用是附带Timer)
                    if(!Relations().get(clientId).equals(targetId)) {
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "402", role, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(this, session, error);
                    } else if(ObjectUtils.isEmpty(message)){
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("error", clientId, targetId, "501", role, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(this, session, error);
                    } else if(Relations().containsKey(targetId)) {
                        PowerBoxDataWithSingleAttachment dataWithAttachment = (PowerBoxDataWithSingleAttachment) data;
                        // AB通道的执行时间可以独立
                        String[] args = (String[])data.getArgsArrayByPointing(PowerBoxDataType.PULSE);
                        char channel = StringHandlerUtil.getCharForString(args[0],0);
                        Integer sendTime = dataWithAttachment.isValid() ? dataWithAttachment.getTimer() : PunishmentTime();
                        ChannelHandlerContext app = Connections().get(targetId);
                        String[] waveDataList = new String[args.length - 1];
                        System.arraycopy(args, 1, waveDataList, 0, args.length - 1);
                        String waveDataListString = PowerBoxData.reformWaveDataList(waveDataList);
                        String messageCommand = "pulse-"+ channel + ":" + waveDataListString;
                        PowerBoxMessage pulseMsg = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, messageCommand, role, new WebSocketApplicationRole("Ap" + targetId));
                        Integer totalSends = PunishmentTime() * sendTime;
                        Integer timesSpace = 1000 / PunishmentTime();

                        logger.debug("频道{}消息发送中，频道消息数：{}，持续时间：{}",channel, totalSends, sendTime);
                        if(ClientTimers().containsKey(clientId)) {
                            //接收消息未工作完毕，清除旧计时器并发送清除App队列消息
                            PowerBoxMessage clearAndUpdateMsg = PowerBoxMessage.createPowerBoxMessage("clientMsg", clientId, targetId, "over-previous-value", role, new WebSocketApplicationRole("Ap" + targetId));
                            sendMessageData(this, session, clearAndUpdateMsg);
                            Timer timerId = ClientTimers().get(clientId + "-" + channel);
                            clearInterval(clientId, timerId, channel);
                            ClientTimers().remove(clientId + "-" + channel);
                            //发送 App波形队列清除指令
                            String commandMsg = "clear-"+(channel == 'A' ? "1": "2") ;//非A即B（
                            PowerBoxMessage clear = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, commandMsg, role, new WebSocketApplicationRole("Ap" + targetId));
                            sendMessageData(this, app, clear);
                            Promise<Void> promise = session.newPromise();
                            ScheduledFuture<?> scheduledFuture = session.executor().schedule(() -> {
                                promise.setSuccess(null);
                            },150, TimeUnit.MILLISECONDS);
                            promise.addListener((FutureListener<Void>) future -> {
                                if (future.isSuccess()) {
                                    delaySendMsg(clientId, session, app, pulseMsg, totalSends, timesSpace, channel);
                                } else logger.error("WTF?(╯‵□′)╯︵┻━┻");
                            });
                        } else delaySendMsg(clientId, session, app, pulseMsg, totalSends, timesSpace, channel);//如果不存在未发送玩的消息 直接发送
                    } else {
                        logger.debug("为找到匹配的客户端,clientId={}", clientId);
                        PowerBoxMessage not_found = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, "404", role, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(this, session, not_found);
                    }
                }
                default -> {
                    if(!Relations().get(clientId).equals(targetId)) {
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "402", role, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(this, session, error);
                    } else if (Connections().containsKey(clientId)) {
                        ChannelHandlerContext client = Connections().get(clientId);
                        PowerBoxMessage defaultMsg = PowerBoxMessage.createPowerBoxMessage(data.getType(), clientId, targetId, data.getMessage(), role, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(this, client, defaultMsg);
                    } else {
                        PowerBoxMessage defaultMsg = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, "404", role, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(this, session, defaultMsg);
                    }
                }
            }
        }
    }

    @Override
    public void ErrorHandler(ChannelHandlerContext session, Throwable cause) {
        // java.io.IOException: 远程主机强迫关闭了一个现有的连接。 这个错误是由于连接直接被关闭 无需处理
        if(cause instanceof IOException) {
            return;
        }
        logger.error("WebSocket 异常{}:{}", cause.getClass(),cause.getMessage());
        //在此通知用户异常，提过Websocket 发送消息给对方
        String _errorSideId_ = ChannelIdMap().get(session.channel().id().asLongText());
        if(ObjectUtils.isEmpty(_errorSideId_)) {
            logger.debug("Can't found the client");
            return;
        }
        //构造错误信息
        String  errorMessage = "Websocket异常 " + cause.getMessage();
        for(String _clientId_ : Relations().keySet()) {
            String _targetId_ = Relations().get(_clientId_);
            if(_clientId_.equals(_errorSideId_)) {//clientId = value
                //通知app
                ChannelHandlerContext appClient = Connections().get(_targetId_);
                PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("error", _clientId_, _targetId_, "500", role, new WebSocketApplicationRole("Ap" + _targetId_));
                sendMessageData(this, appClient, error);
            } else if(_targetId_.equals(_errorSideId_)) {
                //通知client
                ChannelHandlerContext client = Connections().get(_clientId_);
                PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("error", _clientId_ , _targetId_, errorMessage, role, new WebSocketClientRole("Cl" + _targetId_) );
                sendMessageData(this, client, error);
            }
        }
    }
    private void putDataInMap(String targetId, PowerBoxData data) {
        if(PowerBoxDataMap().containsKey(targetId))PowerBoxDataMap().replace(targetId, data);
        else PowerBoxDataMap().put(targetId, data);
    }
    /**
     * 根据UUID对应对象的类型来决定发送的数据类型JSON
     * @param uuid 能处理Message对象的目标UUID
     * @param msg Message{@link Message}
     */
    private static void sendMessageOrData(ServerDLPBHandler serverDLPBHandler, String uuid, Message msg) {
        switch (getRoleType(serverDLPBHandler, uuid)) {
            case T_CLIENT -> sendMessageText(serverDLPBHandler, serverDLPBHandler.Connections().get(uuid), msg);
            case PLACEHOLDER,APPLICATION -> sendMessageData(serverDLPBHandler, serverDLPBHandler.Connections().get(uuid), msg);
        }
    }

    /**
     *
     * @param target 能处理Message对象的目标
     * @param msg Message{@link Message}
     */
    private static void sendMessageText(ServerDLPBHandler serverDLPBHandler, ChannelHandlerContext target, Message msg) {
        if(target == null) throw new NullPointerException("target is null");
        if(msg == null) throw new NullPointerException("message is null");
        try {
            String json = msg.getMsgJson();
            target.channel().writeAndFlush(new TextWebSocketFrame(json)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.debug("消息已成功发送到客户端 UUID={}，消息内容={}", serverDLPBHandler.ChannelIdMap().get(target.channel().id().asLongText()), json);
                } else {
                    logger.error("消息发送失败，客户端 UUID={}，消息内容={}", serverDLPBHandler.ChannelIdMap().get(target.channel().id().asLongText()), json, future.cause());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message{}", e.getMessage());
        }
    }
    /**
     * 发送Message的Data数据
     * @param target 能处理Message对象的目标
     * @param msg 待转化为Message{@link Message}
     */
    protected static void sendMessageData(ServerDLPBHandler serverDLPBHandler, ChannelHandlerContext target, Message msg) {
        if(target == null) throw new NullPointerException("target is null");
        if(msg == null) throw new NullPointerException("message is null");
        try{
            String json = msg.getDataJson();
            target.channel().writeAndFlush(new TextWebSocketFrame(json)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.debug("消息已成功发送到客户端 UUID={}，消息内容={}", serverDLPBHandler.ChannelIdMap().get(target.channel().id().asLongText()), json);
                } else {
                    logger.error("消息发送失败，客户端 UUID={}，消息内容={}", serverDLPBHandler.ChannelIdMap().get(target.channel().id().asLongText()) , json, future.cause());
                }
            });

        }catch (Exception e){
            logger.error("Error sending message{}", e.getMessage());
        }
    }


    public void delaySendMsg(String clientId, ChannelHandlerContext client, ChannelHandlerContext target, Message message, Integer totalSends, Integer timeSpace, char channel) {
        sendMessageData(this, target, message);
        totalSends--;
        if(totalSends> 0 ) {
            Timer timer = new Timer();
            DgLabTimerTask task = new DgLabTimerTask(client, target, message, totalSends, k -> {
                clearInterval(clientId, timer, channel);
            }, channel);
            timer.scheduleAtFixedRate(task, 0, timeSpace.longValue());//周期触发
            ClientTimers().put(clientId + "-" + channel, timer);//存储对应的·clientId与频道
        }
    }

    public void clearInterval(String clientId, Timer timer, char channel) {
        timer.cancel();
        ClientTimers().remove(clientId + "-" + channel); // 删除对应的定时器
    }

    /**
     *
     * @param clientId 客户端uuid
     * @return PowerData 客户端Data消息（存储的一般是对应的pulse数据）
     * @throws NullPointerException 如果对于clientId未存入对应数据
     */
    public static PowerBoxData getData(ServerDLPBHandler serverDLPBHandler, String clientId) throws NullPointerException {
        return serverDLPBHandler.PowerBoxDataMap().get(clientId);
    }


    public ChannelGroup Channel() {
        return SharedData.channels;
    }

    public Map<String, String> ChannelIdMap() {
        return SharedData.channelIdMap;
    }

    public Map<String, ChannelHandlerContext> Connections() {
        return SharedData.connections;
    }

    public Map<String, PowerBoxData> PowerBoxDataMap() {
        return SharedData.powerBoxDataMap;
    }

    public Map<String, String> Relations() {
        return SharedData.relations;
    }

    public Map<String, Timer> ClientTimers() {
        return SharedData.clientTimers;
    }

    public int PunishmentDuration() {
        return SharedData.punishmentDuration;
    }

    public int  PunishmentTime() {
        return SharedData.punishmentTime;
    }

    public Timer HeartTimer() {
        return SharedData.heartTimer;
    }

    //为什么不传入serverDLPBHandler.Relations()而传入整个ServerDLPBHandler（不符合良好的程序开发准则）
    public static RoleType getRoleType(ServerDLPBHandler serverDLPBHandler, String uuid) {
        if(serverDLPBHandler.Relations().containsKey(uuid)) return RoleType.T_CLIENT;
        else if(serverDLPBHandler.Relations().containsValue(uuid)) return RoleType.APPLICATION;
        else return RoleType.PLACEHOLDER;
    }

}
