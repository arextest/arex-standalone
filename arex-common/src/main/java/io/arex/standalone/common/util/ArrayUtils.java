package io.arex.standalone.common.util;

public class ArrayUtils {
    private ArrayUtils() {}

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static byte[] addAll(final byte[] array1, final byte... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        final byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    private static byte[] clone(final byte[] array) {
        if (array == null) {
            return new byte[0];
        }
        return array.clone();
    }

    public static <T> boolean isNotEmpty(final T[] array) {
        return (array != null && array.length != 0);
    }
}
