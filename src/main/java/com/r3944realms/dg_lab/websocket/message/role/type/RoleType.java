package com.r3944realms.dg_lab.websocket.message.role.type;

public enum RoleType {
    /**
     * Tradition Server 传统意义上的服务器
     */
    T_SERVER,
    /**
     * Tradition Client 传统意义上的客户端
     */
    T_CLIENT,
    /**
     * App 应用
     */
    APPLICATION,
    /**
     * 占位符 即任意端
     */
    PLACEHOLDER;
    public static RoleType getTypeFromString(String string) {
        return switch (string) {
            case "T_SERVER" -> T_SERVER;
            case "T_CLIENT" -> T_CLIENT;
            case "APPLICATION" -> APPLICATION;
            case "PLACEHOLDER" -> PLACEHOLDER;
            default -> null;
        };
    }
}
