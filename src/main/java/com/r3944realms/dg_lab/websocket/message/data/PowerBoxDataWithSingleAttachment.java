package com.r3944realms.dg_lab.websocket.message.data;

public class PowerBoxDataWithSingleAttachment extends PowerBoxData {
    private final Integer timer;
    public PowerBoxDataWithSingleAttachment(PowerBoxData parent, Integer timer) {
        super(parent.getType(), parent.getClientId(), parent.getTargetId(), parent.getMessage());
        this.timer = timer;
    }
    public Integer getTimer() {
        return timer;
    }
    public static PowerBoxDataWithSingleAttachment attach(PowerBoxData parent, Integer timer){
        if(parent == null)
            throw new NullPointerException("parent is null");
        return new PowerBoxDataWithSingleAttachment(parent, timer);
    }
    @Override
    public boolean isValid() {
        return super.isValid() && timer != null;
    }
    @Override
    public DataType Type() {
        return DataType.POWER_BOX_ATT;
    }
}
