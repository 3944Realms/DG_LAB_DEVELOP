package com.r3944realms.dg_lab.future.websocket;


import com.r3944realms.dg_lab.Dg_Lab;
import com.r3944realms.dg_lab.future.misc.Status;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.protocol.HttpRequestHandler;
import com.r3944realms.dg_lab.websocket.utils.RangeValidator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * AbstractWebSocketServer
 */
@SuppressWarnings({"DuplicatedCode", "LoggingSimilarMessage"})
public abstract class AbstractWebSocketServer {

    protected static Logger logger =  LoggerFactory.getLogger(AbstractWebSocketServer.class);

    private int Port;

    protected ServerBootstrap ServerBootstrap;
    protected EventLoopGroup BossGroup, WorkerGroup;
    protected Channel ServerChannel;

    protected Thread WebsocketServerThread;
    protected volatile Status ServerStatus = Status.WAITING_FOR_INIT;
    protected AbstractWebSocketServer() {
        Port = 9000;
    }
    protected AbstractWebSocketServer(int port) {
        Port = port;
    }

    public Status getStatus() {
        return this.ServerStatus;
    }
    public void setPort(int port) {
        if(this.ServerStatus != Status.WAITING_FOR_INIT || this.ServerStatus != Status.STOPPED) {
            logger.error("Unable to Change Port to {}", port);
            return;
        }
        this.Port = RangeValidator.isValidPort(port) ? port : 9000;
    }
    public int getPort() {
        return Port;
    }

    /**
     * 提供实现此处添加对Message处理Handler
     * @param pipeline 管道
     */
    protected abstract void MessagePipeLineHandler(ChannelPipeline pipeline);
    protected void initThread0() {}
    protected final void initThread() {
        BossGroup = new NioEventLoopGroup();
        WorkerGroup = new NioEventLoopGroup();
        WebsocketServerThread = new Thread(() ->{
            try {
                ServerBootstrap = new ServerBootstrap();
                ServerStatus = Status.STARTING;
                ServerBootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);
                ServerBootstrap.group(BossGroup, WorkerGroup);
                ServerBootstrap.channel(NioServerSocketChannel.class);
                ServerBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(Dg_Lab.LOGGING_HANDLER);
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new HttpRequestHandler());//去除路径请求，APP会发送带路径请求的HTTP升级请求，目前用不到
                        pipeline.addLast("WSP",new WebSocketServerProtocolHandler("/"));
                        pipeline.addLast(new WebSocketFrameAggregator(65536));
                        MessagePipeLineHandler(pipeline);
                    }
                });
                logger.debug("WebSocketServer try binding port ... ");
                ChannelFuture channelFuture = ServerBootstrap.bind(Port);
                channelFuture.sync();
                ServerChannel = channelFuture.channel();
                ServerStatus = Status.RUNNING;
                logger.info("WebSocketServer start on the port of {}", Port);
                logger.debug("WebSocketServer listening on port {}", Port);
                channelFuture.channel().closeFuture().sync();

            } catch (Exception e) {
                logger.error(e.getMessage());
            } finally {
                stop();
            }
        }, "DG_LABWebSocketServer");
        initThread0();
    }

    /**
     * 自定义Staret后续逻辑
     */
    protected void start0() {}
    public final void start() {
        switch (ServerStatus) {
            case STARTING -> logger.info("Server is already starting.");
            case RUNNING -> logger.info("Server is already running.");
            case STOPPING -> logger.info("Server is stopping");
            case STOPPED, WAITING_FOR_INIT -> {
                initThread();
                WebsocketServerThread.start();
                start0();
            }
        }
    }

    /**
     * 自定义Stop后续逻辑
     */
    protected CompletableFuture<Void> stop0() { return CompletableFuture.runAsync(() -> {}); }
    public final void stop() {
        switch (ServerStatus) {
            case WAITING_FOR_INIT -> logger.warn("Not Init. (It shouldn't be happened)");
            case STARTING -> logger.info("Server is starting, please waiting.");
            case RUNNING -> {
                ServerStatus = Status.STOPPING;
                // Close the server channel if it's open
                CompletableFuture<Void> SerChanClose = CompletableFuture.runAsync(() -> {
                    if (ServerChannel != null && ServerChannel.isOpen()) {
                        try {
                            ServerChannel.close().sync();
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage());
                        } finally {
                            ServerChannel = null;
                        }
                    }
                });
                // Shutdown the event loop groups
                CompletableFuture<Void> BosGroClose = SerChanClose.thenRunAsync(() -> {
                            if (BossGroup != null) {
                                try {
                                    BossGroup.shutdownGracefully().sync();
                                } catch (InterruptedException e) {
                                    logger.error(e.getMessage());
                                } finally {
                                    BossGroup = null;
                                }
                            }
                        }
                );
                CompletableFuture<Void> WorGroClose = SerChanClose.thenRunAsync(() -> {
                    if (WorkerGroup != null) {
                        try {
                            WorkerGroup.shutdownGracefully().sync();
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage());
                        } finally {
                            WorkerGroup = null;
                        }
                    }
                });
                BosGroClose.thenCombineAsync(WorGroClose, (a,b) -> {
                    stop0().thenRunAsync(() -> {
                        ServerStatus = Status.STOPPED;
                        ServerBootstrap = null;
                    });
                    return null;
                });

            }
            case STOPPING -> logger.info("Server is already stopping");
            case STOPPED -> logger.info("Server has stopped");
        }
    }

    public abstract void send(String clientId, Message message);
}
