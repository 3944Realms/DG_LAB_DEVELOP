package com.r3944realms.dg_lab;

/**
 * 语言键值
 */
public class APILanguageKey {
    private final static String API_WEBSOCKET_ = "whimsy.api.websocket.",
                                POWER_BOX_ = "power_box.";
    /**
     * PB_LINK_OF_QRCODE -> PowerBox显示链接 <br/>
     * PB_BIND_SUCCESSFUL -> PowerBox绑定成功
     */
    public final static String PB_LINK_OF_QRCODE = API_WEBSOCKET_ + POWER_BOX_ + "link.show",
                                PB_BIND_SUCCESSFUL = API_WEBSOCKET_ + POWER_BOX_ + "bind.successful";
}
