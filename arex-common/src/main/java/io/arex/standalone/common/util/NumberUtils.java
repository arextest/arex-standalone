package io.arex.standalone.common.util;

public class NumberUtils {
    public static boolean isDigits(String str) {
        if (StringUtil.isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotDigits(String str) {
        return !isDigits(str);
    }
}
