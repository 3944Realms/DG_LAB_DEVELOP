package com.r3944realms.dg_lab.websocket.message.data;

import com.r3944realms.dg_lab.websocket.message.data.type.PowerBoxDataType;
import com.r3944realms.dg_lab.websocket.message.data.type.PowerBoxStatusCode;
import com.r3944realms.dg_lab.websocket.utils.stringUtils.StringHandlerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("FieldCanBeLocal")
public class PowerBoxData implements IData {
    private final String type;
    private final String clientId;
    private final String targetId;
    private final String message;
    public PowerBoxData(String type, String clientId, String targetId, String message) {
        this.type = type;
        this.clientId = clientId;
        this.targetId = targetId;
        this.message = message;
    }
    public static PowerBoxData createPowerBoxData(String type, String clientId, String targetId, String message) {
        return new PowerBoxData(type, clientId, targetId, message);
    }

    public String getType() {
        return type;
    }
    public String getClientId() {
        return clientId;
    }
    public String getTargetId() {
        return targetId;
    }
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isValid() {
        if(type == null || type.isEmpty() || clientId == null  || targetId == null || message == null) {
            inValidReason.set("Invalid PowerBox Data");
            return false;
        }
        final boolean commonValidCheck = !clientId.isEmpty() && !targetId.isEmpty() && !message.isEmpty();
        return switch (type) {
            case "heartbeat" -> !clientId.isEmpty() && PowerBoxStatusCode.isValidStatusCode(message);
            case "bind" -> Objects.equals(message, "targetId") ? (targetId.isEmpty() && !clientId.isEmpty()) : commonValidCheck;
            case "msg" -> !clientId.isEmpty() && !targetId.isEmpty() && isCommandValid(message);
            case "break","clientMsg" -> commonValidCheck;
            case "error" -> !message.isEmpty();
            default -> false;
        };
    }

    @Override
    public DataType Type() {
        return DataType.POWER_BOX;
    }

    public PowerBoxDataType getCommandType() {
        return PowerBoxDataType.getType(type, message);
    }
    @SuppressWarnings("DuplicatedCode")
    public Object[] getArgsArrayByPointing(PowerBoxDataType dataType) {
        if(message == null || message.isEmpty()) {
            return null;
        }
        String[] args = message.split("-");
        switch(dataType) {
            case STRENGTH: {
                String[] arguments = args[1].split("\\+");
                int argumentsLength = arguments.length;
                switch (argumentsLength) {
                    case 3:{
                        int channel = Integer.parseInt(arguments[0]);
                        int strengthChangePolicy = Integer.parseInt(arguments[1]);
                        int value = Integer.parseInt(arguments[2]);
                        return new Integer[]{channel, strengthChangePolicy, value};
                    }
                    case 4:{
                        int AStrength = Integer.parseInt(arguments[0]);
                        int BStrength = Integer.parseInt(arguments[1]);
                        int ALimit = Integer.parseInt(arguments[2]);
                        int BLimit = Integer.parseInt(arguments[3]);
                        return new Integer[]{AStrength, BStrength, ALimit, BLimit};
                    }
                }
            }
            case PULSE: {
                String channel = args[1].substring(0,1);
                String[] DataList = getWaveformDataList(args[1]);
                String[] dataList = new String[DataList.length + 1];
                int i = 0;
                dataList[i] = channel;
                for(String str : DataList) {
                    i++;
                    dataList[i] = str;
                }
                return dataList;

            }
            case CLEAR: {
                String arg = args[1];
                return new Integer[]{ Integer.parseInt(args[1]) };
            }
            case FEEDBACK:{
                int arg = Integer.parseInt(args[1]);
                return new Integer[]{ arg };
            }
            default: return null;
        }
    }
    @SuppressWarnings("DuplicatedCode")
    public Object[] getArgsArray() {
        if(message == null || message.isEmpty()) {
            return null;
        }
        String[] args = message.split("-");
        switch(args[0]) {
            case "strength": {
                String[] arguments = args[1].split("\\+");
                int argumentsLength = arguments.length;
                switch (argumentsLength) {
                    case 3:{
                        int channel = Integer.parseInt(arguments[0]);
                        int strengthChangePolicy = Integer.parseInt(arguments[1]);
                        int value = Integer.parseInt(arguments[2]);
                        return new Integer[]{channel, strengthChangePolicy, value};
                    }
                    case 4:{
                        int AStrength = Integer.parseInt(arguments[0]);
                        int BStrength = Integer.parseInt(arguments[1]);
                        int ALimit = Integer.parseInt(arguments[2]);
                        int BLimit = Integer.parseInt(arguments[3]);
                        return new Integer[]{AStrength, BStrength, ALimit, BLimit};
                    }
                }
            }
            case "pulse": {
                String channel = args[1].substring(0,1);
                String[] DataList = getWaveformDataList(args[1]);
                String[] dataList = new String[DataList.length + 1];
                int i = 0;
                dataList[i] = channel;
                for(String str : DataList) {
                    i++;
                    dataList[i] = str;
                }
                return dataList;

            }
            case "clear": {
                String arg = args[1];
                return new Integer[]{ Integer.parseInt(args[1]) };
            }
            case "feedback":{
                int arg = Integer.parseInt(args[1]);
                return new Integer[]{ arg };
            }
            default: return null;
        }
    }

    public boolean isCommandValid(String command) {
        if(command == null || command.isEmpty()) {
            return false;
        }
        String[] args = command.split("-");
        try {
            switch(args[0]) {
                case "strength": {
                    String[] arguments = args[1].split("\\+");
                    int argumentsLength = arguments.length;
                    switch (argumentsLength) {
                        case 3:{
                            int channel = Integer.parseInt(arguments[0]);
                            if(channel != 1 && channel != 2) throw new IllegalArgumentException("Channel must be 1 or 2");
                            int strengthChangePolicy = Integer.parseInt(arguments[1]);
                            if(2 < strengthChangePolicy || strengthChangePolicy < 0) throw new IllegalArgumentException("Strength change policy must in the range of [0,2]");
                            int value = Integer.parseInt(arguments[2]);
                            if (value < 0 || value > 200) throw new IllegalArgumentException("Value must be between 0 and 200");
                            return true;
                        }
                        case 4:{
                            return true;//App发来的数据应该不会有问题（如果有也不是我的锅（））
                        }
                        default: throw new IllegalArgumentException("Invalid number of arguments");
                    }
                }
                case "pulse": {
                    String channel = args[1].substring(0,1);
                    if(!(channel.equals("A") || channel.equals("B"))) throw new IllegalArgumentException("Channel is incorrect or lacked.");
                    String[] DataList = getWaveformDataList(args[1]);
                    Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{8}$");//检查是否为16进制数字（大小写都可以）
                    if (DataList.length > 100) throw new IllegalArgumentException("The list of Waveform data is too long.");
                    for(String str : DataList) {
                        if(str.length() != 8) {
                            throw new IllegalArgumentException("Find list has a the invalid length of waveform data.");
                        }
                        Matcher matcher = pattern.matcher(str);
                        if (!matcher.matches()) {
                            throw new NumberFormatException("Find list has a incorrect syntax of waveform data.");
                        }
                    }
                    return true;
                }
                case "clear": {
                    String arg = args[1];
                    if(args.length != 2) throw new IllegalArgumentException("Invalid number of arguments");
                    if(!Objects.equals(arg, "1") && !Objects.equals(arg, "2")) throw new IllegalArgumentException("The argument must be 1 or 2");
                    return true;
                }
                case "feedback":{
                    int arg = Integer.parseInt(args[1]);
                    if(args.length != 2) throw new IllegalArgumentException("Invalid number of arguments");
                    if(0 > arg || arg > 10) throw new IllegalArgumentException("args must be between 0 and 10");
                    return true;
                }
                default: throw new IllegalArgumentException("Invalid command");
            }
        } catch (Exception e) {
            inValidReason.set(e.getMessage());
            return false;//指令不正确，直接否
        }
    }

    String[] getWaveformDataList(String msg) {
        String dataList = msg.substring(msg.indexOf('[') + 1, msg.indexOf(']'));
        String[] rawStringList = dataList.split(",");
        ArrayList<String> list = new ArrayList<>();
        Arrays.stream(rawStringList).forEach(rawString -> {
            list.add(rawString.replaceAll("\"", ""));
        });
        String[] result = new String[list.size()];
        list.toArray(result);
        return result;
    }
    /**
     * 将String[] 转化为 <code>["xxxxxxxxxxxxxxxx",......,"xxxxxxxxxxxxxxxx"]</code><br/><br/>
     * <b>例 :</b><code>[ff00ff, ac013021, 23990123]</code> -> <code>["ff00ff","ac013021","23990123"]</code>
     */
    public static String reformWaveDataList(String[] waveDataList) {
        List<String> b = new ArrayList<>(Arrays.asList(StringHandlerUtil.addQuotes(waveDataList)));
        return b.toString().replace(" ","");//去除空格
    }

    public static void main(String[] args) {
        String[] strings = new String[]{"ff00ff00", "00ff00ff", "ff00ff00", "12345678"};
        System.out.println(reformWaveDataList(strings));
    }
}
