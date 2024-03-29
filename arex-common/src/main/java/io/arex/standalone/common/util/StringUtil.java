package io.arex.standalone.common.util;

import io.arex.agent.thirdparty.util.CharSequenceUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;

public class StringUtil {
    public static final String EMPTY = "";
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final int INDEX_NOT_FOUND = -1;

    public static String defaultString(final String str) {
        return str == null ? EMPTY : str;
    }

    public static String defaultString(final String str, String def) {
        return isEmpty(str) ? def : str;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for(int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static String[] split(final String source, final char separator) {
        if (isEmpty(source)) {
            return EMPTY_STRING_ARRAY;
        }

        final int len = source.length();
        final List<String> list = new ArrayList<>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (source.charAt(i) == separator) {
                if (match) {
                    list.add(source.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || lastMatch) {
            list.add(source.substring(start, i));
        }
        return list.toArray(new String[0]);
    }

    public static String join(final Iterable<?> iterable, final String separator) {
        if (iterable == null) {
            return null;
        }

        Iterator<?> iterator = iterable.iterator();

        if (!iterator.hasNext()) {
            return null;
        }

        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            return Objects.toString(first, "");
        }

        final StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }

    private static void indent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
    }

    public static Map<String, String> asMap(String content) {
        if (isEmpty(content)) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        for (String str : content.split(";")) {
            if (isEmpty(str)) {
                continue;
            }
            String[] arr = str.split("=");
            if (arr.length != 2) {
                continue;
            }

            final String k = arr[0];
            final String v = arr[1];
            if (isNotEmpty(k) && isNotEmpty(v)) {
                map.put(k, v);
            }
        }
        return map;
    }

    // net.bytebuddy auto modified by shade: shaded.net.bytebuddy
    public static String removeShadePrefix(String str) {
        return str.length() > 7 ? str.substring(7) : str;
    }

    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        } else {
            if (start < 0) {
                start += str.length();
            }

            if (start < 0) {
                start = 0;
            }

            return start > str.length() ? "" : str.substring(start);
        }
    }

    public static String[] splitByWholeSeparator(String str, String separator) {
        if (str == null) {
            return new String[0];
        } else {
            int len = str.length();
            if (len == 0) {
                return new String[0];
            } else {
                int separatorLength = separator.length();
                List<String> substrings = new ArrayList<>();
                int beg = 0;
                int end = 0;

                while(end < len) {
                    end = str.indexOf(separator, beg);
                    if (end > -1) {
                        if (end > beg) {
                            substrings.add(str.substring(beg, end));
                            beg = end + separatorLength;
                        } else {
                            beg = end + separatorLength;
                        }
                    } else {
                        substrings.add(str.substring(beg));
                        end = len;
                    }
                }

                return substrings.toArray(new String[0]);
            }
        }
    }

    public static int encodeAndHash(String str){
        if (isBlank(str)) {
            return 0;
        }
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8)).hashCode();
    }

    public static String replaceEach(final String text, final String[] searchList, final String[] replacementList,
                                      final boolean repeat, final int timeToLive) {
        if (timeToLive < 0) {
            final Set<String> searchSet = new HashSet<>(Arrays.asList(searchList));
            final Set<String> replacementSet = new HashSet<>(Arrays.asList(replacementList));
            searchSet.retainAll(replacementSet);
            if (!searchSet.isEmpty()) {
                throw new IllegalStateException("Aborting to protect against StackOverflowError - " +
                        "output of one loop is the input of another");
            }
        }

        if (isEmpty(text) || searchList == null || searchList.length == 0 ||
                replacementList == null || replacementList.length == 0) {
            return text;
        }

        final int searchLength = searchList.length;
        final int replacementLength = replacementList.length;

        // make sure lengths are ok, these need to be equal
        if (searchLength != replacementLength) {
            throw new IllegalArgumentException("Search and Replace array lengths don't match: "
                    + searchLength + " vs " + replacementLength);
        }

        final boolean[] noMoreMatchesForReplIndex = new boolean[searchLength];

        int textIndex = -1;
        int replaceIndex = -1;
        int tempIndex = -1;

        for (int i = 0; i < searchLength; i++) {
            if (noMoreMatchesForReplIndex[i] || isEmpty(searchList[i]) || replacementList[i] == null) {
                continue;
            }
            tempIndex = text.indexOf(searchList[i]);

            if (tempIndex == -1) {
                noMoreMatchesForReplIndex[i] = true;
            } else if (textIndex == -1 || tempIndex < textIndex) {
                textIndex = tempIndex;
                replaceIndex = i;
            }
        }

        if (textIndex == -1) {
            return text;
        }

        int start = 0;
        int increase = 0;

        for (int i = 0; i < searchList.length; i++) {
            if (searchList[i] == null || replacementList[i] == null) {
                continue;
            }
            final int greater = replacementList[i].length() - searchList[i].length();
            if (greater > 0) {
                increase += 3 * greater;
            }
        }
        increase = Math.min(increase, text.length() / 5);

        final StringBuilder buf = new StringBuilder(text.length() + increase);
        while (textIndex != -1) {

            for (int i = start; i < textIndex; i++) {
                buf.append(text.charAt(i));
            }
            buf.append(replacementList[replaceIndex]);

            start = textIndex + searchList[replaceIndex].length();

            textIndex = -1;
            replaceIndex = -1;
            for (int i = 0; i < searchLength; i++) {
                if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                        searchList[i].isEmpty() || replacementList[i] == null) {
                    continue;
                }
                tempIndex = text.indexOf(searchList[i], start);

                // see if we need to keep searching for this
                if (tempIndex == -1) {
                    noMoreMatchesForReplIndex[i] = true;
                } else if (textIndex == -1 || tempIndex < textIndex) {
                    textIndex = tempIndex;
                    replaceIndex = i;
                }
            }
        }

        final int textLength = text.length();
        for (int i = start; i < textLength; i++) {
            buf.append(text.charAt(i));
        }
        final String result = buf.toString();
        if (!repeat) {
            return result;
        }

        return replaceEach(result, searchList, replacementList, repeat, timeToLive - 1);
    }

    public static boolean containsIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (regionMatches(str, true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    public static boolean startWithFrom(String source, String prefix, int checkStartIndex) {
        int length = prefix.length();
        if (checkStartIndex < 0 || checkStartIndex + length > source.length()) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (prefix.charAt(i) != source.charAt(checkStartIndex++)) {
                return false;
            }
        }
        return true;
    }

    static boolean regionMatches(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int start, int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String)cs).regionMatches(ignoreCase, thisStart, (String)substring, start, length);
        } else {
            int index1 = thisStart;
            int index2 = start;
            int tmpLen = length;
            int srcLen = cs.length() - thisStart;
            int otherLen = substring.length() - start;
            if (thisStart >= 0 && start >= 0 && length >= 0) {
                if (srcLen >= length && otherLen >= length) {
                    while(tmpLen-- > 0) {
                        char c1 = cs.charAt(index1++);
                        char c2 = substring.charAt(index2++);
                        if (c1 != c2) {
                            if (!ignoreCase) {
                                return false;
                            }

                            if (Character.toUpperCase(c1) != Character.toUpperCase(c2) && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                                return false;
                            }
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public static String format(String from, Object... arguments) {
        if (StringUtil.isEmpty(from)) {
            return EMPTY;
        }
        String computed = from;
        if (arguments != null && arguments.length != 0) {
            for (Object argument : arguments) {
                if (argument == null) {
                    continue;
                }
                computed = computed.replaceFirst("\\{\\}", Matcher.quoteReplacement(argument.toString()));
            }
        }
        return computed;
    }

    public static boolean equalsIgnoreCase(final CharSequence str1, final CharSequence str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2;
        } else if (str1 == str2) {
            return true;
        } else if (str1.length() != str2.length()) {
            return false;
        } else {
            return CharSequenceUtils.regionMatches(str1, true, 0, str2, 0, str1.length());
        }
    }

    public static String substringBetween(final String str, final String open, final String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        final int start = str.indexOf(open);
        if (start != INDEX_NOT_FOUND) {
            final int end = str.indexOf(close, start + open.length());
            if (end != INDEX_NOT_FOUND) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    public static String[] substringsBetween(final String str, final String open, final String close) {
        if (str == null || isEmpty(open) || isEmpty(close)) {
            return null;
        }
        final int strLen = str.length();
        if (strLen == 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final int closeLen = close.length();
        final int openLen = open.length();
        final List<String> list = new ArrayList<String>();
        int pos = 0;
        while (pos < strLen - closeLen) {
            int start = str.indexOf(open, pos);
            if (start < 0) {
                break;
            }
            start += openLen;
            final int end = str.indexOf(close, start);
            if (end < 0) {
                break;
            }
            list.add(str.substring(start, end));
            pos = end + closeLen;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.toArray(new String [list.size()]);
    }

    public static String replace(final String text, final String searchString, final String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    public static String replace(final String text, final String searchString, final String replacement, int max) {
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == INDEX_NOT_FOUND) {
            return text;
        }
        final int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        final StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != INDEX_NOT_FOUND) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }
}
