package com.r3944realms.dg_lab.future.websocket.hanlder;

public interface ClientOperation {
    void createQrCode(String qrCodeUrl);
    void inform();
    void notice();
}
