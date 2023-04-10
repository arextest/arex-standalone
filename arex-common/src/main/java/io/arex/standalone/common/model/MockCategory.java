package io.arex.standalone.common.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MockCategory implements Serializable {
    private static final Map<String, MockCategory> CATEGORY_TYPE_MAP = new HashMap<>();
    public static final MockCategory SERVLET = createEntryPoint("Servlet");
    public static final MockCategory DATABASE = createDependency("Database");
    public static final MockCategory HTTP_CLIENT = createDependency("HttpClient");
    public static final MockCategory CONFIG_FILE = createSkipComparison("ConfigFile");
    public static final MockCategory DYNAMIC_CLASS = createSkipComparison("DynamicClass");
    public static final MockCategory REDIS = createSkipComparison("Redis");
    public static final MockCategory MESSAGE_PRODUCER = createDependency("QMessageProducer");
    public static final MockCategory MESSAGE_CONSUMER = createEntryPoint("QMessageConsumer");
    public static final MockCategory DUBBO_CONSUMER = createDependency("DubboConsumer");
    public static final MockCategory DUBBO_PROVIDER = createEntryPoint("DubboProvider");
    public static final MockCategory DUBBO_STREAM_PROVIDER = createDependency("DubboStreamProvider");

    private String name;
    private boolean entryPoint;
    private boolean skipComparison;

    public static MockCategory createEntryPoint(String name) {
        return create(name, true, false);
    }

    public static MockCategory createSkipComparison(String name) {
        return create(name, false, true);
    }

    public static MockCategory createDependency(String name) {
        return create(name, false, false);
    }

    public static MockCategory create(String name, boolean entryPoint, boolean skipComparison) {
        return CATEGORY_TYPE_MAP.computeIfAbsent(name,
                key -> new MockCategory(name, entryPoint, skipComparison));
    }

    public static Collection<MockCategory> values() {
        return CATEGORY_TYPE_MAP.values();
    }

    public String getName() {
        return this.name;
    }

    public boolean isEntryPoint() {
        return this.entryPoint;
    }

    public boolean isSkipComparison() {
        return this.skipComparison;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEntryPoint(boolean entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void setSkipComparison(boolean skipComparison) {
        this.skipComparison = skipComparison;
    }

    public MockCategory() {
    }

    private MockCategory(String name, boolean entryPoint, boolean skipComparison) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.skipComparison = skipComparison;
    }

    public static MockCategory getByName(String name) {
        return CATEGORY_TYPE_MAP.get(name);
    }
}
