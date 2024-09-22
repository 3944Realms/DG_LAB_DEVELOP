package com.r3944realms.dg_lab.websocket.protocol;

import com.google.gson.JsonSyntaxException;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.message.MessageDirection;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxDataWithSingleAttachment;
import com.r3944realms.dg_lab.websocket.message.data.type.PowerBoxDataType;
import com.r3944realms.dg_lab.websocket.message.role.PlaceholderRole;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketApplicationRole;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketServerRole;
import com.r3944realms.dg_lab.websocket.timeTask.DgLabTimerTask;
import com.r3944realms.dg_lab.websocket.utils.annoation.NeedCompletedInFuture;
import com.r3944realms.dg_lab.websocket.utils.stringUtils.StringHandlerUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.r3944realms.dg_lab.websocket.WebSocketServer.*;
import static com.r3944realms.dg_lab.websocket.protocol.ServerMessageDataTextWebsocketHandler.sendMessageData;

/**
 * 针对Netty客户端发送的是MessageJson，对App发送DataJson
 */
@SuppressWarnings("DuplicatedCode")
@NeedCompletedInFuture(futureTarget = "减少二者冗余部分，用基类来同一行为，提取更多方法来实现功能化")
public class ServerMessageTextWebsocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    //消息发送错误信息专用logger
    private static final Logger errorReport = LoggerFactory.getLogger(ServerMessageTextWebsocketHandler.class);


    static Logger logger = LoggerFactory.getLogger(ServerMessageTextWebsocketHandler.class);

    //处理连接
    @Override
    public void handlerAdded(ChannelHandlerContext session) throws Exception {
        String clientId = UUID.randomUUID().toString();
        channelIdMap.put(session.channel().id().asLongText(), clientId);
        logger.debug("channel added: clientId={}", clientId);
        channels.add(session.channel());
        connections.put(clientId, session);
        logger.info("新的 webSocket 连接已建立, 标识符为{}", clientId);
    }

    @Override
    public void channelActive(ChannelHandlerContext session) throws Exception {
        super.channelActive(session);
        String clientId = channelIdMap.get(session.channel().id().asLongText());
        PowerBoxMessage bindMsg = PowerBoxMessage.createPowerBoxMessage("bind", clientId, "", "targetId",
                SOCKET_SERVER_ROLE, new PlaceholderRole("Pl" + clientId)
        );
        // 延迟发送消息
        session.executor().schedule(() -> {
            if (session.channel().isActive() && session.channel().isOpen()) {
                session.channel().writeAndFlush(new TextWebSocketFrame(bindMsg.getDataJson())).addListener(sendFuture -> {
                    if (sendFuture.isSuccess()) {
                        logger.info("Message sent successfully to clientId={}", clientId);
                    } else {
                        logger.error("Failed to send message to clientId={}", clientId, sendFuture.cause());
                    }
                });
            } else {
                logger.debug("Channel is not active, message not sent to clientId={}", clientId);
            }
        }, 200, TimeUnit.MILLISECONDS); // 延迟500毫秒发送消息
        if(heartTimer != null) return;
        synchronized (ServerMessageTextWebsocketHandler.class) {
            // 启动心跳定时器（如果尚未启动）
            if (heartTimer == null) {
                heartTimer = new Timer();
                heartTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!connections.isEmpty()) {
                            logger.debug("关系池大小：{}连接池大小：{},发送心跳消息", relations.size(), connections.size());
                        }
                        for (String clientId : connections.keySet()) {
                            ChannelHandlerContext client = connections.get(clientId);
                            if(client != null && client.channel().isActive() && client.channel().isOpen()) {
                                String targetId = relations.get(clientId);
                                if (ObjectUtils.isEmpty(targetId)) {
                                    targetId = "";
                                }PowerBoxMessage message =
                                        PowerBoxMessage.createPowerBoxMessage("heartbeat", clientId, targetId, "200",
                                                SOCKET_SERVER_ROLE, new PlaceholderRole("Pl" + clientId));
                                sendMessageText(client, message);
                            } else {
                                logger.debug("Channel is not active, skipping heartbeat for clientId={}", clientId);
                                connections.remove(clientId);//不活跃移除对应连接
                            }
                        }
                    }
                }, 500, 60000/*ms = 1min*/);
            }
        }
    }

    //处理关闭
    @Override
    public void handlerRemoved(ChannelHandlerContext session) throws Exception {
        logger.debug("Websocket 连接已关闭");
        String channelId = session.channel().id().asLongText();
        String disconnectId = channelIdMap.remove(channelId);
        logger.debug("channel removed: channelId={}", channelId);
        logger.info("断开的client Id:{}", disconnectId);
        channels.remove(session.channel());
        for(String _clientId_ : relations.keySet()) {
            String _targetId_ = relations.get(_clientId_);
            if(_clientId_.equals(disconnectId)) {
                //Client断开 ，通知app
                ChannelHandlerContext app = connections.get(_clientId_);
                PowerBoxMessage message =
                        PowerBoxMessage.createPowerBoxMessage("break", disconnectId, _targetId_, "209", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + _targetId_));
                sendMessageData(app,message);
                app.close(); //关闭当前的 websocket 连接
                relations.remove(_clientId_); // 清除关系
                powerBoxDataMap.remove(_targetId_);
                logger.debug("Close Application Connecting{}", _targetId_);
                connections.remove(_targetId_);
            } else if(_targetId_.equals(disconnectId)) {
                //app断开,通知Client
                ChannelHandlerContext client = connections.get(_clientId_);
                PowerBoxMessage message =
                        PowerBoxMessage.createPowerBoxMessage("break", _targetId_, _clientId_, "209", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + disconnectId));
                sendMessageText(client, message);
                client.close();//关闭当前的 websocket 连接
                relations.remove(_clientId_);// 清除关系
                powerBoxDataMap.remove(_targetId_);
                logger.debug("Close Client Connecting{}", _targetId_);
                connections.remove(_clientId_);
            }
            connections.remove(disconnectId);//清除ws客户端
            channels.remove(session.channel());
            logger.debug("channel removed: channelId={}[Current Size:{}]", channelId, connections.size());
        }
    }
    //处理错误
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // java.io.IOException: 远程主机强迫关闭了一个现有的连接。 这个错误是由于连接直接被关闭 无需处理
        if(cause instanceof IOException) {
            return;
        }
        logger.error("WebSocket 异常{}:{}", cause.getClass(), cause.getMessage());
        //在此通知用户异常，提过Websocket 发送消息给对方
        String clientId = channelIdMap.get(ctx.channel().id().asLongText());
        if(ObjectUtils.isEmpty(clientId)) {
            logger.debug("Can't found the client");
            return;
        }
        //构造错误信息
        String  errorMessage = "Websocket异常 " + cause.getMessage();
        for(String _clientId_ : relations.keySet()) {
            String _targetId_ = relations.get(_clientId_);
            if(_clientId_.equals(clientId)) {//clientId = value
                //通知app
                ChannelHandlerContext app = connections.get(_targetId_);
                PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("error", clientId, _targetId_, "500", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + _targetId_));
                sendMessageData(app, error);
            } else if(_targetId_.equals(clientId)) {
                //通知client
                ChannelHandlerContext client = connections.get(_clientId_);
                PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("error", _clientId_, _targetId_, errorMessage, SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + _targetId_) );
                sendMessageText(client, error);
            }
        }

    }
    //读入
    @Override
    protected void channelRead0(ChannelHandlerContext session, TextWebSocketFrame msg) throws Exception {
        logger.debug("收到消息：{}", msg);
        PowerBoxMessage pMessage;
        PowerBoxData data = null;
        PowerBoxMessage dataMessage = null;
        try {
            try {
                dataMessage = PowerBoxMessage.getNullMessage().getMessage(msg.text());
                if(dataMessage == null) throw new NullPointerException("incorrect syntax");
            } catch (JsonSyntaxException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException e) {
                data = PowerBoxMessage.getNullMessage().getPayload(msg.text());
                if(data == null) throw new NullPointerException("Not Json Msg");
                MessageDirection<WebSocketApplicationRole, WebSocketServerRole> direction = new MessageDirection<>(new WebSocketApplicationRole("Ap" + data.getTargetId()) ,SOCKET_SERVER_ROLE);
                dataMessage = new PowerBoxMessage(data, direction);
            }
            assert data != null;
            MessageDirection<PlaceholderRole, WebSocketServerRole> direction = new MessageDirection<>(
                    new PlaceholderRole("Pl" + data.getClientId()), SOCKET_SERVER_ROLE
            );
            pMessage = new PowerBoxMessage(data, direction);
        } catch (Exception e) { //非JSON消息
            pMessage = PowerBoxMessage.createPowerBoxMessage("error", "", "",  "403",
                    SOCKET_SERVER_ROLE, new WebSocketClientRole("ErrorReceiver"));
            sendMessageText(session, pMessage);
            return;
        }
        if(!connections.containsKey(data.getClientId()) && !connections.containsKey(data.getTargetId())) { //非法信息来源拒绝
            pMessage = PowerBoxMessage.createPowerBoxMessage("error", "", "", "404",
                    SOCKET_SERVER_ROLE, new WebSocketClientRole("ErrorReceiver"));
            sendMessageText(session, pMessage);
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
                    if (connections.containsKey(clientId) && connections.containsKey(targetId)){
                        //relations的双方都不存在这对id 且 信息为DGLAB
                        if(!(relations.containsKey(clientId) || relations.containsKey(targetId) || relations.containsValue(clientId) || relations.containsValue(targetId)) && message.equals("DGLAB")) {
                            relations.put(clientId, targetId);
                            ChannelHandlerContext client = connections.get(clientId);
                            PowerBoxMessage bindMessage = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "200", SOCKET_SERVER_ROLE, new PlaceholderRole("Both: " + clientId + " ^ " + targetId));
                            sendMessageData(session, bindMessage);
                            sendMessageText(client, bindMessage);
                        } else {
                            PowerBoxMessage failure = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "400", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                            sendMessageData(session, failure);
                        }
                    } else {
                        PowerBoxMessage failure = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "401", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                        sendMessageData(session, failure);
                    }
                }
                case STRENGTH,PULSE,CLEAR,FEEDBACK -> {
                    if(relations.containsKey(clientId) && !relations.get(clientId).equals(targetId)) {//没有绑定关系
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "402", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                        sendMessageData(session, error);
                        return;
                    }
                    Object[] argsArray = data.getArgsArray();
                    if(argsArray == null) {
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, "500", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                        sendMessageData(session, error);
                        return;
                    }
                    switch (dataType) {
                        case STRENGTH ->{
                            if(connections.containsKey(targetId)) {
                                ChannelHandlerContext app = connections.get(targetId);
                                ChannelHandlerContext client = connections.get(clientId);
                                if(argsArray.length == 3){
                                    int channel = ((Integer[])argsArray)[0];
                                    int policyChange = ((Integer[])argsArray)[1];
                                    int strengthChangeValue = ((Integer[])argsArray)[2];
                                    String messageCommand = "strength-" + channel + "+" + policyChange + "+" + strengthChangeValue;
                                    PowerBoxMessage strengthUpdate = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, messageCommand, SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                                    sendMessageData(app, strengthUpdate);
                                } else if(argsArray.length == 4){
                                    int AStrength = ((Integer[])argsArray)[0];
                                    int BStrength = ((Integer[])argsArray)[1];
                                    int ALimit = ((Integer[])argsArray)[2];
                                    int BLimit = ((Integer[])argsArray)[3];
                                    putDataInMap(targetId, data);
                                    String currentStrengthMsg = "strength-" + AStrength + "+" + BStrength + "+" + ALimit + "+" + BLimit;
                                    PowerBoxMessage clientMsg = PowerBoxMessage.createPowerBoxMessage("clientMsg", clientId, targetId, currentStrengthMsg, SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                                    sendMessageText(client, clientMsg);
                                }
                            }
                        }
                        case PULSE -> {
                            //Thrown UnsupportedMsg
                            PowerBoxMessage UnsupportedMsg = PowerBoxMessage.createPowerBoxMessage("error", clientId, targetId, "502", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                            //"unsupported-reason:(please use type named clientMsg to adjust wave in order to get the timers of AB)"
                            sendMessageText(session, UnsupportedMsg);//发送给客户端
                        }
                        case CLEAR -> {
                            if(connections.containsKey(targetId)) {
                                ChannelHandlerContext app = connections.get(targetId);
                                String Channel = ((String[])argsArray)[0];
                                String messageCommand = "clear-" + Channel;
                                PowerBoxMessage strengthUpdate = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, messageCommand, SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                                sendMessageData(app, strengthUpdate);
                            }
                        }
                        case FEEDBACK -> {
                            if(connections.containsKey(targetId)) { //发送给客户端
                                int index = ((Integer[])argsArray)[0];
                                String feedBack = "feedback-" + index;
                                PowerBoxMessage feedBackMsg = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, feedBack, SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                                sendMessageData(clientId, feedBackMsg);
                            }
                        }
                    }

                }
                case CLIENT_MESSAGE -> {//接收到客户端消息(客户端的作用是附带Timer)
                    if(!relations.get(clientId).equals(targetId)) {
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "402", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(session, error);
                    } else if(ObjectUtils.isEmpty(message)){
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("error", clientId, targetId, "501", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(session, error);
                    } else if(connections.containsKey(targetId)) {
                        PowerBoxDataWithSingleAttachment dataWithAttachment = (PowerBoxDataWithSingleAttachment)data;
                        // AB通道的执行时间可以独立
                        String[] args = (String[])data.getArgsArrayByPointing(PowerBoxDataType.PULSE);
                        char channel = StringHandlerUtil.getCharForString(args[0],0);
                        Integer sendTime = 0;
                        sendTime = dataWithAttachment.isValid() ? dataWithAttachment.getTimer() : punishmentDuration;
                        ChannelHandlerContext app = connections.get(targetId);
                        String[] waveDataList = new String[args.length - 1];
                        System.arraycopy(args, 1, waveDataList, 0, args.length - 1);
                        String waveDataListString = PowerBoxData.reformWaveDataList(waveDataList);
                        String messageCommand = "pulse-"+ channel + ":" + waveDataListString;
                        PowerBoxMessage pulseMsg = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, messageCommand, SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                        Integer totalSends = punishmentTime * sendTime;
                        Integer timesSpace = 1000 / punishmentTime;

                        logger.debug("频道{}消息发送中，频道消息数：{}，持续时间：{}",channel, totalSends, sendTime);
                        if(clientTimers.containsKey(clientId)) {
                            //接收消息未工作完毕，清除旧计时器并发送清除App队列消息
                            PowerBoxMessage clearAndUpdateMsg = PowerBoxMessage.createPowerBoxMessage("clientMsg", clientId, targetId, "over-previous-value", SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                            sendMessageData(session, clearAndUpdateMsg);
                            Timer timerId = clientTimers.get(clientId + "-" + channel);
                            clearInterval(clientId, timerId, channel);
                            clientTimers.remove(clientId + "-" + channel);
                            //发送 App波形队列清除指令
                            String commandMsg = "clear-"+(channel == 'A' ? "1": "2") ;//非A即B（
                            PowerBoxMessage clear = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, commandMsg, SOCKET_SERVER_ROLE, new WebSocketApplicationRole("Ap" + targetId));
                            sendMessageData(app, clear);
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
                        PowerBoxMessage not_found = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, "404", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(session, not_found);
                    }
                }
                default -> {
                    if(!relations.get(clientId).equals(targetId)) {
                        PowerBoxMessage error = PowerBoxMessage.createPowerBoxMessage("bind", clientId, targetId, "402", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(session, error);
                    } else if (connections.containsKey(clientId)) {
                        ChannelHandlerContext client = connections.get(clientId);
                        PowerBoxMessage defaultMsg = PowerBoxMessage.createPowerBoxMessage(data.getType(), clientId, targetId, data.getMessage(), SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(client, defaultMsg);
                    } else {
                        PowerBoxMessage defaultMsg = PowerBoxMessage.createPowerBoxMessage("msg", clientId, targetId, "404", SOCKET_SERVER_ROLE, new WebSocketClientRole("Cl" + clientId));
                        sendMessageText(session, defaultMsg);
                    }
                }
            }
        }
    }

    private static void putDataInMap(String targetId, PowerBoxData data) {
        if(powerBoxDataMap.containsKey(targetId))powerBoxDataMap.replace(targetId, data);
        else powerBoxDataMap.put(targetId, data);
    }

    /**
     *
     * @param uuid 能处理Message对象的目标UUID（UUID必须存在）
     * @param msg 待转化为Message{@link Message}
     */
    private static void sendMessageText(String uuid, Message msg) {
        ChannelHandlerContext target = connections.get(uuid);
        if(target == null) throw new NullPointerException("No connection for uuid: " + uuid);
        try {
            String json = msg.getMsgJson();
            target.channel().writeAndFlush(new TextWebSocketFrame(json)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.debug("消息已成功发送到客户端 UUID={}，消息内容={}", uuid, json);
                } else {
                    logger.error("消息发送失败，客户端 UUID={}，消息内容={}", uuid, json, future.cause());
                }
            });
        } catch (Exception e) {
            ErrorLogger(e);
        }
    }
    /**
     *
     * @param target 能处理Message对象的目标
     * @param msg Message{@link Message}
     */
    private static void sendMessageText(ChannelHandlerContext target, Message msg) {
        if(target == null) throw new NullPointerException("target is null");
        if(msg == null) throw new NullPointerException("message is null");
        try {
            String json = msg.getMsgJson();
            target.channel().writeAndFlush(new TextWebSocketFrame(json)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.debug("消息已成功发送到客户端 UUID={}，消息内容={}", channelIdMap.get(target.channel().id().asLongText()), json);
                } else {
                    logger.error("消息发送失败，客户端 UUID={}，消息内容={}", channelIdMap.get(target.channel().id().asLongText()), json, future.cause());
                }
            });
        } catch (Exception e) {
            ErrorLogger(e);
        }
    }


    private static void ErrorLogger(Exception e) {
        logger.error("Error sending message{}", e.getMessage());
    }
    public void delaySendMsg(String clientId, ChannelHandlerContext client, ChannelHandlerContext target, Message message, Integer totalSends, Integer timeSpace, char channel) {
        sendMessageData(target, message);
        totalSends--;
        if(totalSends> 0 ) {
            Timer timer = new Timer();
            DgLabTimerTask task = new DgLabTimerTask(client, target, message, totalSends, k -> {
                clearInterval(clientId, timer, channel);
            }, channel);
            timer.scheduleAtFixedRate(task, 0, timeSpace.longValue());//周期触发
            clientTimers.put(clientId + "-" + channel, timer);//存储对应的·clientId与频道
        }
    }
    public void clearInterval(String clientId, Timer timer, char channel) {
        timer.cancel();
        clientTimers.remove(clientId + "-" + channel); // 删除对应的定时器
    }

    /**
     *
     * @param clientId 客户端uuid
     * @return PowerData 客户端Data消息（存储的一般是对应的pulse数据）
     * @throws NullPointerException 如果对于clientId未存入对应数据
     */
    public static PowerBoxData getData(String clientId) throws NullPointerException {
        return powerBoxDataMap.get(clientId);
    }

}


