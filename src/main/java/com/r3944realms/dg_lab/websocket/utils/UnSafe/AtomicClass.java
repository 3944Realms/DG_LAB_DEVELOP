package com.r3944realms.dg_lab.websocket.utils.UnSafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

@SuppressWarnings("FieldMayBeFinal")
public class AtomicClass<T> {
    private static final VarHandle VALUE;
    private volatile T value;
    public AtomicClass(T value) {
        this.value = value;
    }
    static {
        try {
            Field valueField = AtomicClass.class.getDeclaredField("value");
            VALUE = MethodHandles.lookup().unreflectVarHandle(valueField);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        while (true) {
            //noinspection unchecked
            T prev = (T) VALUE.getVolatile(this);
            if (VALUE.compareAndSet(this, prev, newValue)) {
                break;
            }
        }
    }
}
