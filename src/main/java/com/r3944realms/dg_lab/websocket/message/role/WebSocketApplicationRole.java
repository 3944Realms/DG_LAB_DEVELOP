package com.r3944realms.dg_lab.websocket.message.role;

import com.r3944realms.dg_lab.websocket.message.role.type.RoleType;

public class WebSocketApplicationRole extends Role{
    public WebSocketApplicationRole(String name) {
        super(name, RoleType.APPLICATION);
    }
}
