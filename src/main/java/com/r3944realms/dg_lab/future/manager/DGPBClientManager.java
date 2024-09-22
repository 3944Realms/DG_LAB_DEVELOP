package com.r3944realms.dg_lab.future.manager;

import com.r3944realms.dg_lab.future.websocket.PowerBoxWSClient;

public class DGPBClientManager implements IDG_LabManager{
    protected PowerBoxWSClient client;
    public DGPBClientManager(PowerBoxWSClient client) {
        this.client = client;
    }
    @Override
    public void start() {
        client.start();
    }

    @Override
    public void stop() {
        client.stop();
    }

}
