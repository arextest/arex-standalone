package io.arex.standalone.local.util;

import io.arex.agent.bootstrap.util.StringUtil;

public class JsonUtil {
    public static String formatJson(String jsonStr) {
        if (StringUtil.isEmpty(jsonStr)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        char last = '\0';
        char current = '\0';
        int indent = 0;
        boolean isInQuotationMarks = false;
        for (int i = 0; i < jsonStr.length(); i++) {
            last = current;
            current = jsonStr.charAt(i);
            switch (current) {
                case '"':
                    if (last != '\\') {
                        isInQuotationMarks = !isInQuotationMarks;
                    }
                    sb.append(current);
                    break;
                case '{':
                case '[':
                    sb.append(current);
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent++;
                        indent(sb, indent);
                    }
                    break;
                case '}':
                case ']':
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent--;
                        indent(sb, indent);
                    }
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    if (last != '\\' && !isInQuotationMarks) {
                        sb.append('\n');
                        indent(sb, indent);
                    }
                    break;
                default:
                    sb.append(current);
            }
        }

        return sb.toString();
    }

    private static void indent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
    }

    public static String breakLine(String content, int len) {
        String tmp = "";
        if (len > 0) {
            if (content.length() > len) {
                int rows = (content.length() + len - 1) / len;
                for (int i = 0; i < rows; i++) {
                    if (i == rows - 1) {
                        tmp += content.substring(i * len);
                    } else {
                        tmp += content.substring(i * len, i * len + len) + "\r\n";
                    }
                }
            } else {
                tmp = content;
            }
        }
        return tmp;
    }
}
