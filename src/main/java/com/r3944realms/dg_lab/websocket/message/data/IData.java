package com.r3944realms.dg_lab.websocket.message.data;

import com.google.gson.annotations.Expose;
import com.r3944realms.dg_lab.websocket.utils.UnSafe.AtomicClass;

public interface IData {
    @Expose(serialize = false, deserialize = false)
    AtomicClass<String> inValidReason  = new AtomicClass<>("Invalid arguments [Default Reason]");
    /**
     * 获取 无效原因
     * @return inValidReason 无效原因
     */
    default String getInvalidReason() {
        return inValidReason.get();
    }
    /**
     * 当前数据是否有效
     * @return isValid 有效与否
     */
    boolean isValid();
    /**
     * 获取当前数据类型
     * @return DateType 数据类型
     */
    DataType Type();
    enum DataType {
        POWER_BOX,
        POWER_BOX_ATT,
        DEFAULT;
        public static DataType getTypeFromString(String type) {
            return switch (type) {
                case "POWER_BOX" -> POWER_BOX;
                case "POWER_BOX_ATT" -> POWER_BOX_ATT;
                default -> DEFAULT;
            };
        }
    }
}
