
package org.spf4j.jmx;

import javax.management.InvalidAttributeValueException;

public interface ExportedValue<T> {
    
    String getName();
    
    String getDescription();
    
    T get();
    
    void set(T value) throws InvalidAttributeValueException;
    
    boolean isWriteable();
    
    Class<? extends T> getValueClass();
}
