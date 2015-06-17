
package org.spf4j.configDiscovery.maven.plugin;

/**
 *
 * @author zoly
 */
public final class FieldInfo {

    private final Class<?> type;

    private final Object defaultValue;

    private static final Object NONE = new Object();

    public FieldInfo(final Class<?> type, final Object defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public FieldInfo(final Class<?> type) {
        this.type = type;
        this.defaultValue = NONE;
    }


    public Class<?> getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean hasDefaultValue() {
        return defaultValue != NONE;
    }


    @Override
    public String toString() {
        return "FieldInfo{" + "type=" + type + ", defaultValue=" + defaultValue + '}';
    }





}
