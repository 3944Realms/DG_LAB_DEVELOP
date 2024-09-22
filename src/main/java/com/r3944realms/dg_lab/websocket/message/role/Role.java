package com.r3944realms.dg_lab.websocket.message.role;

import com.r3944realms.dg_lab.websocket.message.role.type.RoleType;
import org.checkerframework.checker.units.qual.N;

import java.io.Serializable;

public abstract class Role implements Serializable {
    public String name;
    public final RoleType type;
    Role(String name, final RoleType type) {
        this.name = name;
        this.type = type;
    }
    public void UpdateName(String Name) {
        this.name = Name;
    }
}

