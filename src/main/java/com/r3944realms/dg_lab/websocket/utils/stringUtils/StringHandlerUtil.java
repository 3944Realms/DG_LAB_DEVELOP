package com.r3944realms.dg_lab.websocket.utils.stringUtils;

public class StringHandlerUtil {
    public static char getCharForString(String str, int index) {
        return str.charAt(index);
    }

    public static String buildWebSocketURL(String address, int port, boolean ssl) {
        String scheme = ssl ? "wss" : "ws";
        return String.format("%s://%s:%d/", scheme, address, port);
    }

    public static String[] addQuotes(String[] arr) {
        String[] newArr = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = "\"" + arr[i] + "\"";
        }
        return newArr;
    }

    public static String[] removeQuotes(String[] arr) {
        String[] newArr = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = arr[i].substring(1, arr[i].length() - 1);
        }
        return newArr;
    }
}
