package com.r3944realms.dg_lab.future.manager;

import com.r3944realms.dg_lab.future.websocket.PowerBoxWSServer;

public class DGPBServerManager implements IDG_LabManager{
    protected PowerBoxWSServer server;
    public DGPBServerManager(PowerBoxWSServer server) {
        this.server = server;
    }
    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop();
    }
}
