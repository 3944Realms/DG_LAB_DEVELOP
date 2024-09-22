package com.r3944realms.dg_lab.manager;

import com.r3944realms.dg_lab.websocket.WebSocketClient;
import com.r3944realms.dg_lab.websocket.WebSocketServer;
import com.r3944realms.dg_lab.websocket.utils.enums.ManagerResultEnum;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 对于客户端方面需要做些处理才能确保其程序能够稳定运行<br/><br/>
 * 需要设置startSupplier和stopSupplier(交给调用者来处理状态)[通过init]
 */
public enum WebsocketClientManager {
    INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClientManager.class);
    private static final AtomicBoolean hadInit = new AtomicBoolean(false);
    public static WebsocketClientManager getManager() {
        return INSTANCE;
    }
    private final AtomicBoolean shouldStart = new AtomicBoolean(false),
                                waitingForSynchronization = new AtomicBoolean(false);
    /**
     * 当且仅当{@link ManagerResultEnum 状态}为 Successful才会启动或停止
     * 对于start可以设置为<br/>例如：
     * {@snippet lang= java:
     * () -> {
     *          if(!FMLEnvironment.dist.isClient()) { //非客户端退出
     *             logger.error("Distributor is not client");
     *             return ManagerResultEnum.MISMATCHED_ENVIRONMENT_FAILURE;
     *         }
     *         try {
     *             if(Minecraft.getInstance().isSingleplayer()) {//在单人模式下
     *                 WebSocketClient.syncServerData(WebSocketServerConfig.WebSocketServerAddress.get(), WebSocketServerConfig.WebSocketServerPort.get());
     *                 return ManagerResultEnum.SUCCESSFUL;//因为是单人模式,直接从配置中读取Websocket服务器地址和端口
     *             }
     *             if (Minecraft.getInstance().player != null) {//非单人模式
     *                 PacketDistributor.sendToServer(new SyncWebsocketRequestPayload(Minecraft.getInstance().player.getUUID()));//向服务器发送获取同步Websocket服务器地址和端口请求
     *                 return ManagerResultEnum.WAITING_FOR_SYNCHRONIZATION;
     *             }
     *             else throw new NullPointerException();
     *         } catch (NullPointerException e) {
     *             logger.error("Null pointer exception, player is null :", e);
     *             return ManagerResultEnum.FAILURE_FROM_CATCH;
     *         }
     * };
     * }
     * <br/>
     * 对于stop可以设置为<br/>例如:
     * {@snippet lang= java :
     * () -> {
     *     if(!FMLEnvironment.dist.isClient()) { //非客户端退出
     *      logger.error("Distributor is not client");
     *      return ManagerResultEnum.MISMATCHED_ENVIRONMENT_FAILURE;
     * }
     * return ManagerResultEnum.SUCCESSFUL;
     * }
     * }
     */
    private static Supplier<ManagerResultEnum> startSupplier, stopSupplier;
    public boolean getShouldStart() {
        return shouldStart.get();
    }
    public void setShouldStart(boolean shouldStart) {
        this.shouldStart.set(shouldStart);
    }
    public boolean getWaitingForSynchronization() {
        return waitingForSynchronization.get();
    }
    public void setWaitingForSynchronization(boolean waitingForSynchronization) {
        this.waitingForSynchronization.set(waitingForSynchronization);
    }

    public void StartClient() {
        ManagerResultEnum result = StartClient0();
        switch (result) {
            case SUCCESSFUL -> {
                WebSocketClient.Start();
                logger.info("Client successfully started");
            }
            case WAITING_FOR_SYNCHRONIZATION -> {
                shouldStart.set(true);
                logger.info("Waiting for synchronization");
                waitingForSynchronization.set(true);
            }
            case MISMATCHED_ENVIRONMENT_FAILURE -> logger.info("Mismatched environment failure");
            case FAILURE_FROM_CATCH -> logger.info("Failure from catch");
            case NOT_INITIALIZED -> logger.error("ClientManager not initialized, please init() first");
            default -> logger.error("Unexpected Result");
        }
    }

    private ManagerResultEnum StartClient0() {
        if(!hadInit.get()) return ManagerResultEnum.NOT_INITIALIZED;
        refreshBoth();
        ManagerResultEnum managerResultEnum;
        managerResultEnum = startSupplier.get();
        return managerResultEnum;
    }

    private static void refreshBoth() {
        WebSocketClient.refresh();
        WebSocketServer.refresh();
    }

    public void StopClient() {
        ManagerResultEnum result = StopClient0();
        switch (result) {
            case SUCCESSFUL -> {
                WebSocketClient.refresh();
                WebSocketClient.Stop();
                logger.info("Client successfully stopped");
            }
            case MISMATCHED_ENVIRONMENT_FAILURE -> logger.info("Mismatched environment failure");
            case NOT_INITIALIZED ->  logger.error("ClientManager not initialized, please init() first");
            default -> logger.error("Unexpected Result");
        }
    }

    private ManagerResultEnum StopClient0() {
        if(!hadInit.get()) return ManagerResultEnum.NOT_INITIALIZED;
        refreshBoth();
        ManagerResultEnum managerResultEnum;
        managerResultEnum = stopSupplier.get();
        return managerResultEnum;
    }

    /**
     * 接受到同步信息后调用此方法即启动客户端<br/>[仅当ShouldStart为TRUE且为WaitingForSynchronization状态时才可使用]
     * @throws UnsupportedOperationException 非此状态调用则会抛出此异常
     */
    public void SyncedAndStart(){
        if(WebsocketClientManager.INSTANCE.getShouldStart() && WebsocketClientManager.INSTANCE.getWaitingForSynchronization()) {
            WebSocketClient.Start();
            WebsocketClientManager.INSTANCE.setShouldStart(false);
            WebsocketClientManager.INSTANCE.setWaitingForSynchronization(false);
        }
        else throw new UnsupportedOperationException("Not supported because of no Waiting for Synchronization or Shouldn't Start status");
    }

    private static void setStartSupplier(Supplier<ManagerResultEnum> startSupplier) {
        if(startSupplier == null) throw new NullPointerException("startSupplier is null");
        WebsocketClientManager.startSupplier = startSupplier;
    }

    private static void setStopSupplier(Supplier<ManagerResultEnum> stopSupplier) {
        if(stopSupplier == null) throw new NullPointerException("stopSupplier is null");
        WebsocketClientManager.stopSupplier = stopSupplier;
    }
    public static void init(@NotNull Supplier<ManagerResultEnum> startSupplier,
                            @NotNull Supplier<ManagerResultEnum> stopSupplier,
                            @NotNull Supplier<Void> informSupplier,
                            @NotNull Supplier<Void> noticeSupplier,
                            @NotNull Consumer<String> qrCodeProducer) {
        try {
            WebsocketClientManager.setStartSupplier(startSupplier);
            WebsocketClientManager.setStopSupplier(stopSupplier);
            WebSocketClient.init(informSupplier, noticeSupplier, qrCodeProducer);
            hadInit.set(true);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
