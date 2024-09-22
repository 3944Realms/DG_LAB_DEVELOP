package com.r3944realms.dg_lab.future.websocket.hanlder;

import com.r3944realms.dg_lab.future.websocket.sharedData.ClientPowerBoxSharedData;
import com.r3944realms.dg_lab.future.websocket.sharedData.ISharedData;
import com.r3944realms.dg_lab.websocket.WebSocketClient;
import com.r3944realms.dg_lab.websocket.message.PowerBoxMessage;
import com.r3944realms.dg_lab.websocket.message.data.PowerBoxData;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketServerRole;
import com.r3944realms.dg_lab.websocket.message.role.type.RoleType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.SocketException;
import java.util.Objects;
import java.util.Timer;

public class ClientDLPBHandler extends AbstractDgLabPowerBoxHandler implements IAttachSharedData {
    private final ClientPowerBoxSharedData SharedData;
    public final WebSocketClientRole role;
    public final ClientOperation CO;
    public boolean SRMsg;

    public ClientDLPBHandler(ClientPowerBoxSharedData SharedData, WebSocketClientRole role, ClientOperation Co) {
        this(SharedData, role, Co, false);
    }

    public ClientDLPBHandler(ClientPowerBoxSharedData SharedData, WebSocketClientRole role, ClientOperation Co, boolean SRMsg) {
        this.SharedData = SharedData;
        this.role = role;
        this.CO = Co;
        this.SRMsg = SRMsg;
    }

    @Override
    public ISharedData getSharedData() {
        return SharedData;
    }
    @Override
    public void SessionBuildInHandle(ChannelHandlerContext session) {
        logger.info("连接已建立");
    }

    @Override
    public void ActiveSessionHandle(ChannelHandlerContext session) {
        //NOOP
    }

    @Override
    public void SessionCloseHandle(ChannelHandlerContext session) {
        PowerBoxMessage breakMsg = PowerBoxMessage.createPowerBoxMessage("break", ConnectionId(), TargetWSId(), "200", new WebSocketClientRole("Cl" + ConnectionId()), new WebSocketServerRole("WebSocketServer"));
        if(session != null && session.channel().isActive() && session.channel().isOpen()) sendMsgOrData(session, breakMsg);
        logger.info("连接已断开");
    }

    @Override
    public void ReadMsgHandle(ChannelHandlerContext session, TextWebSocketFrame msg) {
        PowerBoxMessage dataMsg;
        PowerBoxData data = null;
        String json = msg.text();
        if(SRMsg && !TargetWSId().isEmpty()) {
            //通过构造开启且如果有目标（即连上了APP端）则开启Message对象校验，只有通过校验才能进读取data(雾)
            dataMsg = PowerBoxMessage.getNullMessage().getMessage(json);
            //发送对象必须是服务器类型 且 接收者（本客户端）为占位对象类型 或 本客户端对象（名字类型相同），这里取反，只有满足条件才能进入 else读取 data
            if(
                    dataMsg.direction.sender().type != RoleType.T_SERVER
                    &&
                    !(
                        dataMsg.direction.receiver().type == RoleType.PLACEHOLDER
                        ||
                        (dataMsg.direction.receiver().type == RoleType.T_CLIENT && Objects.equals(dataMsg.direction.receiver().name, role.name))
                    )
            ) {
                logger.info("消息验证者错误：{}", dataMsg.direction.sender().name);
                data = PowerBoxMessage.getNullMessage().getPayload(dataMsg.getInvalidMessageJson());
            } else {
                data = PowerBoxMessage.getNullMessage().getPayload(json);
            }
        } else {
            data = PowerBoxMessage.getNullMessage().getPayload(json);
        }
        assert data != null;
        switch (data.getCommandType()) {
            case _NC_BIND_ -> {
                if(data.getTargetId().isEmpty()) {
                    //初次连接客户端获取服务器指定id
                    SharedData.connectionId = data.getClientId();
                    logger.info("收到clientId: {}", ConnectionId());
                    String qrCodeContext = "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + WebSocketClient.getUrl()  + ConnectionId();
//                    FilePathHelper.ReCreateHCJFile(qrCodeContext);
                    try {
                        CO.createQrCode(qrCodeContext);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        CO.inform();
                    } catch (Exception e) {
                        logger.debug("Notice File Link Failed");
                    }
                    logger.debug("重新生成QrCodeContext: {}",qrCodeContext);
                } else {
                    if(!Objects.equals(data.getClientId(), ConnectionId())) {
                        logger.debug("接收到不正确的target消息,消息中目标Id{}",data.getClientId());
                        return;
                    }
                    SharedData.targetWSId = data.getTargetId();
                    logger.info("已建立绑定连接，TargetId:{}, msg:{}",TargetWSId(),data.getMessage());
                    try {
//                        Component bind_suc = Component.translatable(APILanguageKey.PB_BIND_SUCCESSFUL);
//                        NoticePlayer.showMessageToLocalPlayer(Minecraft.getInstance().player, bind_suc);
                        CO.notice();
                    } catch (Exception e) {
                        logger.debug("Notice Failed");
                    }
                }
            }
            case _NC_BREAK_ -> {
                //对方断开
                if (!Objects.equals(data.getTargetId(), TargetWSId())) {
                    return;
                }
                logger.info("对方已断开，targetId:{}, msg:{}", TargetWSId(), data.getMessage());
            }
            case _NC_ERROR_ -> {
                if (!Objects.equals(data.getTargetId(), TargetWSId())) {
                    return;
                }
                logger.info("接收到错误码:{}",data.getMessage());
            }
            case _NC_HEARTBEAT_ -> {
                logger.info("收到心跳");
                SharedData.connectionId = data.getClientId();
                role.UpdateName("Cl" + SharedData.connectionId);
            }
            default -> {
                logger.info("收到其它信息{}",data.getMessage());

            }
        }

    }


    @Override
    public void ErrorHandler(ChannelHandlerContext session, Throwable cause) {
        logger.error("连接出现错误：{}",cause.getMessage());
        //错误处理逻辑待加入
        if(cause instanceof SocketException) {
            logger.info("远程服务器关闭");
        }
    }

    private void sendMsgOrData(ChannelHandlerContext target, PowerBoxMessage dataMsg) {
        String json = SRMsg ? dataMsg.getMsgJson() : dataMsg.getDataJson();
        target.channel().writeAndFlush(new TextWebSocketFrame(json));
    }

    public String ConnectionId() {
        return SharedData.connectionId;
    }

    public String TargetWSId() {
        return SharedData.targetWSId;
    }

    public int Delay() {
        return SharedData.delay;
    }

    public Timer DelayTimer() {
        return SharedData.delayTimer;
    }

    public boolean FollowAStrength() {
        return SharedData.followAStrength;
    }

    public boolean FollowBStrength() {
        return SharedData.followBStrength;
    }

}
