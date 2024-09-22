package com.r3944realms.dg_lab.future.websocket.hanlder;

public interface ClientOperation {
    default void createQrCode(String qrCodeUrl) {
        //NOOP
    }
    default void inform() {
        //NOOP
    }
    default void notice() {
        //NOOP
    }
}
