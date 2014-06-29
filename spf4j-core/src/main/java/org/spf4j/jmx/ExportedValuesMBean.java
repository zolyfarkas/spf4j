
package org.spf4j.jmx;


import com.google.common.base.Throwables;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.spf4j.base.Reflections;


class ExportedValuesMBean implements DynamicMBean {

  
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

    private final Map<String, ExportedValue<?>> exported;

    private final ObjectName objectName;

    private final MBeanInfo beanInfo;


    ExportedValuesMBean(final String domain, final String name, final ExportedValue<?> ... exported) {
        this.exported = new HashMap<String, ExportedValue<?>>(exported.length);
        for (ExportedValue<?> val : exported) {
            this.exported.put(val.getName(), val);
        }
        this.objectName = createObjectName(domain, name);
        this.beanInfo = createBeanInfo();
    }

    /**
     * Returns the object name built from the {@link com.netflix.servo.monitor.MonitorConfig}.
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(final String name) throws AttributeNotFoundException {
        ExportedValue<?> result = exported.get(name);
        if (result == null) {
            throw new AttributeNotFoundException(name);
        }
        return result.get();
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(final Attribute attribute)
            throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
        String name = attribute.getName();
        ExportedValue<Object> result = (ExportedValue<Object>) exported.get(name);
        if (result == null) {
            throw new AttributeNotFoundException(name);
        }
        result.set(attribute.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList getAttributes(final String[] names) {
        AttributeList list = new AttributeList(names.length);
        for (String name : names) {
            list.add(new Attribute(name, exported.get(name).get()));
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList setAttributes(final AttributeList list) {
        AttributeList result = new AttributeList(list.size());
        for (Attribute attr : list.asList()) {
            ExportedValue<Object> eval = (ExportedValue<Object>) exported.get(attr.getName());
            if (eval != null) {
                try {
                    eval.set(attr.getValue());
                    result.add(attr);
                } catch (InvalidAttributeValueException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(final String name, final Object[] args, final String[] sig)
            throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException("invoke is not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public MBeanInfo getMBeanInfo() {
        return beanInfo;
    }

    public static ObjectName createObjectName(final String domain, final String name) {
        try {
            final String sanitizedDomain = INVALID_CHARS.matcher(domain).replaceAll("_");
            final String sanitizedName = INVALID_CHARS.matcher(name).replaceAll("_");
            StringBuilder builder = new StringBuilder();
            builder.append(sanitizedDomain).append(':');
            builder.append("name=").append(sanitizedName);
            return new ObjectName(builder.toString());
        } catch (MalformedObjectNameException e) {
            throw Throwables.propagate(e);
        }
    }

    private MBeanInfo createBeanInfo() {
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[exported.size()];
        int i = 0;
        for (ExportedValue<?> val : exported.values()) {
            attrs[i++] = createAttributeInfo(val);
        }
        return new MBeanInfo(
                this.getClass().getName(),
                "MonitorMBean",
                attrs,
                null,  // constructors
                null,  // operators
                null); // notifications
    }

    private static MBeanAttributeInfo createAttributeInfo(final ExportedValue<?> val) {
        Class<?> valClass = Reflections.primitiveToWrapper(val.getValueClass());
        final String type = Number.class.isAssignableFrom(valClass)
            ? Number.class.getName()
            : String.class.getName();
        return new MBeanAttributeInfo(
            val.getName(),
            type,
            val.getDescription(),
            true,   // isReadable
            val.isWriteable(),  // isWritable
            false); // isIs
    }
}
