package io.arex.standalone.common;

import java.util.Base64;

public class CommonUtils {
    public static String decode(String str) {
        try {
            return new String(Base64.getDecoder().decode(str));
        } catch (Exception e) {
            return str;
        }
    }
}
