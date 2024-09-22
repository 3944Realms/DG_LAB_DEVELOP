package com.r3944realms.dg_lab.websocket.utils.stringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlValidator {
    private static final String DOMAIN_REGEX = "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)$";
    private static final String IP_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(DOMAIN_REGEX);
    private static final Pattern IP_PATTERN = Pattern.compile(IP_REGEX);

    public static boolean isValidAddress(String address) {
        return isValidDomain(address) || isValidIP(address);
    }

    private static boolean isValidDomain(String domain) {
        String[] parts = domain.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        for (String part : parts) {
            Matcher matcher = DOMAIN_PATTERN.matcher(part);
            if (!matcher.matches()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIP(String ip) {
        Matcher matcher = IP_PATTERN.matcher(ip);
        return matcher.matches();
    }

}
