
package org.spf4j.jmx;

/**
 *
 * @author zoly
 */
public abstract class PropertySource {

    public abstract Object getProperty(final String name);


    public abstract void setProperty(final String name, final Object value);

}
