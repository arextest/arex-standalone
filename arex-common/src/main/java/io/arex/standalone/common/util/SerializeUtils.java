package io.arex.standalone.common.util;

import io.arex.inst.runtime.serializer.Serializer;
import io.arex.inst.runtime.serializer.StringSerializable;
import io.arex.inst.runtime.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class SerializeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializeUtils.class);

    public static String serialize(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return getSerializer().serialize(object);
        } catch (Throwable ex) {
            LOGGER.warn(LogUtil.buildTitle("serialize"), ex);
            return null;
        }
    }

    public static <T> T deserialize(String value, Class<T> clazz) {
        if (StringUtil.isEmpty(value) || clazz == null) {
            return null;
        }

        try {
            return getSerializer().deserialize(value, clazz);
        } catch (Throwable ex) {
            LOGGER.warn(LogUtil.buildTitle("deserialize-clazz"), ex);
            return null;
        }
    }

    public static <T> T deserialize(String value, Type type) {
        return getSerializer().deserialize(value, type);
    }

    private static StringSerializable getSerializer() {
        return Serializer.getINSTANCE().getSerializer();
    }
}
