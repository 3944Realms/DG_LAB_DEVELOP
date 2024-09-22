package com.r3944realms.dg_lab.websocket.utils;

public class RangeValidator {
    public static boolean isValidPort(int Port) {
        return Port >= 0 && Port <= 65535;
    }
}
