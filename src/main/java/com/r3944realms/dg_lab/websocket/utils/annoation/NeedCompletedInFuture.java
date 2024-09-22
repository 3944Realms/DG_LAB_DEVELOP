package com.r3944realms.dg_lab.websocket.utils.annoation;

public @interface NeedCompletedInFuture {
    String futureTarget() default "";
}
