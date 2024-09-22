package com.r3944realms.dg_lab;

import com.r3944realms.dg_lab.manager.WebsocketClientManager;
import com.r3944realms.dg_lab.manager.WebsocketServerManager;
import com.r3944realms.dg_lab.websocket.WebSocketClient;
import com.r3944realms.dg_lab.websocket.WebSocketServer;
import com.r3944realms.dg_lab.websocket.utils.enums.ManagerResultEnum;
import com.r3944realms.dg_lab.websocket.utils.enums.SendMode;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

//TODO:
// 1.将日志单独在某一个文件里保存
// 2.数据接收能够通知第三方调用者

public class Dg_Lab {
    public static LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);

    /**
     * @param level Netty日志级别()
     */
    public Dg_Lab(LogLevel level) {
        LOGGING_HANDLER = new LoggingHandler(level);
    }

    /**
     * 初始化客户端 WS 管理器
     * @param clientStartSupplier 客户端启动方法(当仅返回Successful才启动WS)
     * @param clientStopSupplier 客户端停止方法(当仅返回Successful才停止WS)
     * @param informSupplier 使用环境下通知操作
     * @param noticeSupplier 调试环境下通知操作
     * @param qrCodeProducer 二维码生成者（传进一个Url，根据Url自定义生成二维码的方式)
     */
    public void initClientManager (
                            @NotNull Supplier<ManagerResultEnum> clientStartSupplier,
                            @NotNull Supplier<ManagerResultEnum> clientStopSupplier,
                            @NotNull Supplier<Void> informSupplier,
                            @NotNull Supplier<Void> noticeSupplier,
                            @NotNull Consumer<String> qrCodeProducer
    ) {
        WebsocketClientManager.init(clientStartSupplier, clientStopSupplier, informSupplier, noticeSupplier, qrCodeProducer);
    }
    /**
     * 初始化客户端 WS
     * @param informSupplier 使用环境下通知操作
     * @param noticeSupplier 调试环境下通知操作
     * @param qrCodeProducer 二维码生成者（传进一个Url，根据Url自定义生成二维码的方式)
     */
    public void initClientWS(
            @NotNull Supplier<Void> informSupplier,
            @NotNull Supplier<Void> noticeSupplier,
            @NotNull Consumer<String> qrCodeProducer
    ) {
        WebSocketClient.init(informSupplier, noticeSupplier, qrCodeProducer);
    }
    /**
     * 获取服务器 Ws 管理器
     * @return manager 管理器
     */
    public WebsocketServerManager getWebsocketServerManager() {
        return WebsocketServerManager.getManager();
    }

    /**
     * 获取客户端 Ws 管理器
     * @return manager 管理器
     */
    public WebsocketClientManager getWebSocketClientManager() {
        return WebsocketClientManager.getManager();
    }
    /**
     * 初始化客户端 配置
     * @param address 链接服务器地址
     * @param port 链接服务器端口
     */
    public void initClientConfig(String address, int port) {
        setConnectTarget(address, port);
    }
    /**
     *设置客户端连接服务器
     * @param address 链接服务器地址
     * @param port 链接服务器端口
     */
    public void setConnectTarget(String address, int port) {
        WebSocketClient.syncServerData(address, port);
    }
    /**
     * 初始化服务器 配置
     * @param port 服务器开放端口
     * @param sendMode 与客户端通讯模式（默认OnlyText)
     */
    public void initServerConfig(int port, SendMode sendMode) {
        BindServerPort(port);
        SetServerSendMode(sendMode);
    }
    /**
     * 服务器 配置
     * @param port 服务器开放端口
     */
    public void BindServerPort(int port) {
        WebSocketServer.BindingPort(port);
    }
    /**
     * 服务器 配置
     * @param sendMode 与客户端通讯模式（默认OnlyText)
     */
    public void SetServerSendMode(SendMode sendMode) {
        WebSocketServer.setMode(sendMode);
    }

}
