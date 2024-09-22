package com.r3944realms.dg_lab.websocket.message.data.type;


public enum PowerBoxDataType {
    _NC_HEARTBEAT_,
    _NC_BIND_,
    _NC_BREAK_,
    _NC_ERROR_,
    STRENGTH(3, 4),
    PULSE(1, 101),
    CLEAR(1),
    FEEDBACK(1),
    CLIENT_MESSAGE,
    UNKNOWN;
    public final int NOP;
    public final int MaxNOP;
    PowerBoxDataType() {
        this(0, 0);
    }
    PowerBoxDataType(int NumberOfParameters) {
        this(NumberOfParameters,-1);
    }
    PowerBoxDataType(int minNumberOfParameters, int maxNumberOfParameters) {
        this.NOP = minNumberOfParameters;
        this.MaxNOP = maxNumberOfParameters;
    }
    private static PowerBoxDataType getCommandType(String commandPrefix) {
        return switch (commandPrefix) {
            case "strength" -> STRENGTH;
            case "pulse" -> PULSE;
            case "clear" -> CLEAR;
            case "feedback" -> FEEDBACK;
            default -> UNKNOWN;
        };
    }
    public static PowerBoxDataType getType(String type, String msg) {
        return switch (type) {
            case "heartbeat" -> _NC_HEARTBEAT_;
            case "bind" -> _NC_BIND_;
            case "msg" -> getCommandType(msg.split("-")[0]);
            case "break" -> _NC_BREAK_;
            case "error" -> _NC_ERROR_;
            case "clientMsg" -> CLIENT_MESSAGE;
            default -> UNKNOWN;
        };
    }
}
