package com.r3944realms.dg_lab.future.websocket;

import com.r3944realms.dg_lab.Dg_Lab;
import com.r3944realms.dg_lab.future.misc.Status;
import com.r3944realms.dg_lab.websocket.message.Message;
import com.r3944realms.dg_lab.websocket.utils.RangeValidator;
import com.r3944realms.dg_lab.websocket.utils.stringUtils.StringHandlerUtil;
import com.r3944realms.dg_lab.websocket.utils.stringUtils.UrlValidator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractWebSocketClient {
    protected static Logger logger = LoggerFactory.getLogger(AbstractWebSocketClient.class);

    private volatile String Address;
    private volatile int Port;

    private Bootstrap ClientBootstrap;
    private EventLoopGroup ClientEventLoopGroup;
    protected Channel ClientChannel;

    private Thread WebSocketClientThread;
    private volatile Status ClientStatus = Status.WAITING_FOR_INIT;

    protected AbstractWebSocketClient() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            this.Address = localHost.getHostAddress();
            this.Port = 9000;
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
    }
    protected AbstractWebSocketClient(int port) {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            this.Address = localHost.getHostAddress();
            this.Port = RangeValidator.isValidPort(port) ? port : 9000;
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
    }
    protected AbstractWebSocketClient(String address, int port) {
        try {
            this.Address = UrlValidator.isValidAddress(address) ? address : InetAddress.getLocalHost().getHostAddress();
            this.Port = RangeValidator.isValidPort(port) ? port : 9000;
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
    }
    public void setPort(int port) {
        if (this.ClientStatus != Status.WAITING_FOR_INIT || this.ClientStatus != Status.STOPPED) {
            logger.error("Unable to change port to {}", port);
            return;
        }
        this.Port = RangeValidator.isValidPort(port) ? port : 9000;
    }
    public void setAddress(String address) {
        if (this.ClientStatus != Status.WAITING_FOR_INIT || this.ClientStatus != Status.STOPPED) {
            logger.error("Unable to change address to {}", address);
            return;
        }
        try {
            this.Address = UrlValidator.isValidAddress(address) ? address : InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    public void setAddressAndPort(String address, int port) {
        try {
            setAddress(address);
            setPort(port);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
    public String getUrl() {
        return StringHandlerUtil.buildWebSocketURL(Address, Port, false);
    }
    /**
     * 提供实现此处添加对Message处理Handler
     * @param pipeline 管道
     */
    protected abstract void MessagePipeLineHandler(ChannelPipeline pipeline);
    protected void initThread0() {}
    protected final void initThread() {
        ClientEventLoopGroup = new NioEventLoopGroup();
        WebSocketClientThread = new Thread(() -> {
            ClientStatus = Status.STARTING;
            ClientBootstrap = new Bootstrap();
            ClientBootstrap.group(ClientEventLoopGroup);
            ClientBootstrap.channel(NioSocketChannel.class);
            ClientBootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(Dg_Lab.LOGGING_HANDLER,
                            new HttpClientCodec(),
                            new HttpObjectAggregator(65536),
                            new WebSocketClientProtocolHandler(
                                    WebSocketClientHandshakerFactory.newHandshaker(
                                            URI.create(StringHandlerUtil.buildWebSocketURL(Address, Port, false)),
                                            WebSocketVersion.V13,
                                            null,
                                            false,
                                            new DefaultHttpHeaders()
                                    )
                            ),
                            new WebSocketFrameAggregator(65536));
                    MessagePipeLineHandler(pipeline);
                }
            });
            try {
                ClientChannel = ClientBootstrap.connect(Address, Port).sync().channel();
                ClientChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            } finally {
                stop();
            }

        });
        initThread0();
    }
    protected void start0() {}
    public final void start() {
        switch (ClientStatus) {
            case STARTING -> logger.info("Client is already starting.");
            case RUNNING -> logger.info("Client is already running.");
            case STOPPING -> logger.info("Client is stopping");
            case STOPPED, WAITING_FOR_INIT -> {
                initThread();
                WebSocketClientThread.start();
                start0();
            }
        }
    }

    protected CompletableFuture<Void> stop0() {
        return CompletableFuture.runAsync(() -> {});
    }
    public final void stop() {
        switch (ClientStatus) {
            case WAITING_FOR_INIT -> logger.warn("Not Init. (It shouldn't be happened)");
            case STARTING -> logger.info("Client is starting, please waiting.");
            case RUNNING -> {
                ClientStatus = Status.STOPPING;
                CompletableFuture<Void> CliChanClose = CompletableFuture.runAsync(() -> {
                    if (ClientChannel != null && ClientChannel.isOpen()) {
                        try {
                            ClientChannel.close().sync();
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage());
                        } finally {
                            ClientChannel = null;
                        }
                    }
                });
                CompletableFuture<Void> CliEvenClose = CliChanClose.thenRunAsync(() -> {
                    if (ClientEventLoopGroup != null) {
                        try {
                            ClientEventLoopGroup.shutdownGracefully().sync();
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage());
                        } finally {
                            ClientEventLoopGroup = null;
                        }
                    }
                });
                CliEvenClose.thenCombineAsync(stop0(), (a, b) -> {
                    ClientStatus = Status.STOPPED;
                    ClientBootstrap = null;
                    return null;
                });
            }
            case STOPPING -> logger.info("Server is already stopping");
            case STOPPED -> logger.info("Server has stopped");
        }
    }
    public abstract void send(Message message);
}
