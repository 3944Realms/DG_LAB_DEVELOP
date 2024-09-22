package com.r3944realms.dg_lab.websocket.utils.UnSafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @deprecated
 */
@Deprecated(since = "jdk18" ,forRemoval = true)
public class UnsafeAccessor {
    private static final Unsafe unsafe;
    static {
        try{
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
