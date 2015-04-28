
package org.spf4j.jmx;

import java.util.Map;
import javax.management.InvalidAttributeValueException;

/**
 *
 * @author zoly
 */
public final class MapExportedValue implements ExportedValue {

    private final Map<String, Object> map;
    private final Map<String, String> descriptions;
    private final String name;

    public MapExportedValue(final Map<String, Object> map, final Map<String, String> descriptions,
            final String name) {
        this.map = map;
        this.descriptions = descriptions;
        this.name = name;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        if (descriptions != null) {
            return descriptions.get(name);
        } else {
            return "";
        }
    }

    @Override
    public Object get() {
        return map.get(name);
    }

    @Override
    public void set(final Object value) throws InvalidAttributeValueException {
        map.put(name, value);
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    @Override
    public Class getValueClass() {
        Object obj = map.get(name);
        if (obj == null) {
            return String.class;
        } else {
            return obj.getClass();
        }
    }

}
