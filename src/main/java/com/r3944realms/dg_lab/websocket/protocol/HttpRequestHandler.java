package com.r3944realms.dg_lab.websocket.protocol;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 读取原始请求的 URI
        String requestUri = request.uri();
        // 修改 URI，去掉路径部分，只保留 "/"
        String modifiedUri = "/";

        // 检查是否是 WebSocket 升级请求
        String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
        String webSocketVersion = request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_VERSION);

        if ("WebSocket".equalsIgnoreCase(upgradeHeader) &&
                "13".equals(webSocketVersion)) {  // 注意：WebSocket 版本通常为 "13"

            // 创建一个新的 FullHttpRequest 实例，修改 URI
            FullHttpRequest modifiedRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    request.method(),
                    modifiedUri,
                    request.content().retain(),  // 保留内容以避免意外释放
                    request.headers().copy(),
                    request.trailingHeaders().copy()
            );

            // 将修改后的 URI 设置到上下文中
            ctx.channel().attr(AttributeKey.valueOf("ws-uri")).set(modifiedUri);

            // 传递修改后的请求到下一个处理器
            ctx.fireChannelRead(modifiedRequest);
        } else {
            // 如果不是 WebSocket 升级请求，返回拒绝
            sendForbiddenResponse(ctx, request);
        }
    }
    private void sendForbiddenResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.content().writeBytes("Access denied".getBytes());

        // Write the response and close the connection
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);
        ctx.close();
    }
}
