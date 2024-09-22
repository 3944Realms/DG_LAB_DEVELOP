package com.r3944realms.dg_lab.future.websocket.hanlder;

public class ExampleOperation implements ClientOperation{
    @Override
    public void createQrCode(String qrCodeUrl) {
        //NOOP
        System.out.println(qrCodeUrl);
    }

    @Override
    public void inform() {
        //NOOP
    }

    @Override
    public void notice() {
        //NOOP
    }
}
