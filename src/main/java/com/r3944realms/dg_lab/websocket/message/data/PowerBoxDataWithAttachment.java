package com.r3944realms.dg_lab.websocket.message.data;
@Deprecated
public class PowerBoxDataWithAttachment extends PowerBoxData {
    private final Integer timer_A;
    private final Integer timer_B;
    public PowerBoxDataWithAttachment(PowerBoxData parent, Integer timer_A, Integer timer_B) {
        super(parent.getType(), parent.getClientId(), parent.getTargetId(), parent.getMessage());
        this.timer_A = timer_A;
        this.timer_B = timer_B;
    }
    public static PowerBoxDataWithAttachment attach(PowerBoxData parent, Integer timer_A, Integer timer_B) {
        if(parent == null)
            throw new NullPointerException("parent is null");
        return new PowerBoxDataWithAttachment(parent, timer_A, timer_B);
    }
    public Integer getTimer_A() {
        return timer_A;
    }
    public Integer getTimer_B() {
        return timer_B;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && (timer_A != null && timer_B != null);
    }

    @Override
    public DataType Type() {
        return DataType.POWER_BOX_ATT;
    }
}
