package com.r3944realms.dg_lab.websocket.utils.enums;

public enum SendMode {
    /**
     * 仅文本 <br/>
     * 即双端均用统一的格式发送Json信息<br/>
     * <br/>
     * PowerBox通讯<br/>Json如下：<br/>
     * <code>{"type":"XXX",clientId:"XXX-XXX-XXX-XXX",targetId:"XXX-XXX-XXX-XXX","message":"XXX"}</code>
     * <br/>详细可见{@link com.r3944realms.dg_lab.websocket.message.data.PowerBoxData}
     */
    OnlyText,
    /**
     * 对客户端使用包含更多信息Message<br/>
     * 仅客户端与服务器通讯采用，app端仍然为原Text格式<br/>
     * 客户端与服务器端PowerBox通讯<br/>Json如下: <br/>
     * <code>{"commandType":"XXX","direction":{"sender":{"name":"uuid","type":"T_CLIENT"},"receiver":{"name":"uuid","type":"T_SERVER"}},"payload":{"timer_A":0,"timer_B":0,"type":"","clientId":"XXX-XXX-XXX-XXX","targetId":"XXX-XXX-XXX-XXX","message":"XXX"}}</code>
     * <br/>详细可见{@link com.r3944realms.dg_lab.websocket.message.PowerBoxMessage}
     */
    ClientMessage
}
