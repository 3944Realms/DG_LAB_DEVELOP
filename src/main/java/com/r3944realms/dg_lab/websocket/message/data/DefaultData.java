package com.r3944realms.dg_lab.websocket.message.data;

public class DefaultData implements IData {
    protected String msg;

    DefaultData() {
        this.msg = "empty";
    }

    DefaultData(String msg) {
        this.msg = msg;
    }

    @Override
    public boolean isValid() {
        return msg != null && !msg.isEmpty();
    }

    @Override
    public DataType Type() {
        return DataType.DEFAULT;
    }

}
