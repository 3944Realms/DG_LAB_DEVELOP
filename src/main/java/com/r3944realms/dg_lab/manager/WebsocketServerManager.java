package com.r3944realms.dg_lab.manager;

import com.r3944realms.dg_lab.websocket.WebSocketServer;
import com.r3944realms.dg_lab.websocket.utils.enums.ManagerResultEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 暂时无需额外处理
 */
public enum WebsocketServerManager {
    INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger(WebsocketServerManager.class);
    public static WebsocketServerManager getManager() {
        return INSTANCE;
    }
    public void StartServer() {
        ManagerResultEnum result = StartServer0();
        switch (result) {
            case SUCCESSFUL -> {
                WebSocketServer.Start();
                logger.info("Server started");
            }
            default -> logger.error("Unexpected Result");
        }
    }
    private ManagerResultEnum StartServer0() {
        return ManagerResultEnum.SUCCESSFUL;
    }
    public void StopServer() {
        ManagerResultEnum result = StopServer0();
        switch (result) {
            case SUCCESSFUL -> {
                WebSocketServer.Stop();
                logger.info("Server started");
            }
            default -> logger.error("Unexpected Result");
        }
    }
    private ManagerResultEnum StopServer0() {
        return ManagerResultEnum.SUCCESSFUL;
    }
}
