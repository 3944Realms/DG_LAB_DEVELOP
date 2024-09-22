package com.r3944realms.dg_lab.future.websocket.hanlder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDgLabPowerBoxHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    public static Logger logger = LoggerFactory.getLogger(AbstractDgLabPowerBoxHandler.class);

    /**
     * 连接建立时触发
     * @param session {@link ChannelHandlerContext Context}
     */
    public abstract void SessionBuildInHandle(ChannelHandlerContext session);

    /**
     * 连接存活时触发
     * @param session {@link ChannelHandlerContext Context}
     */
    public abstract void ActiveSessionHandle(ChannelHandlerContext session);
    /**
     * 连接关闭时触发
     * @param session {@link ChannelHandlerContext Context}
     */
    public abstract void SessionCloseHandle(ChannelHandlerContext session);
    /**
     * 连接获取信息时触发
     * @param session {@link ChannelHandlerContext Context}
     */
    public abstract void ReadMsgHandle(ChannelHandlerContext session, TextWebSocketFrame frame);
    /**
     * 连接出现错误时触发
     * @param session {@link ChannelHandlerContext Context}
     */
    public abstract void ErrorHandler(ChannelHandlerContext session, Throwable cause);

    @Override
    public void handlerAdded(ChannelHandlerContext session) throws Exception {
        super.handlerAdded(session);
        SessionBuildInHandle(session);
    }

    @Override
    public void channelActive(ChannelHandlerContext session) throws Exception {
        super.channelActive(session);
        ActiveSessionHandle(session);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext session) throws Exception {
        super.handlerRemoved(session);
        SessionCloseHandle(session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext session, Throwable cause) throws Exception {
        ErrorHandler(session, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext session, TextWebSocketFrame msg) throws Exception {
        ReadMsgHandle(session, msg);
    }


}
