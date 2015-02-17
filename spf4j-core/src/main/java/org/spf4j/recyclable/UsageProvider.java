package org.spf4j.recyclable;

/**
 *
 * @author zoly
 */
public interface UsageProvider<T> {

    long getUsage(T object);

}
