
package org.spf4j.io;

/**
 *
 * @author zoly
 */
public interface ObjectAppenderSupplier {
 
    <T> ObjectAppender<T> get(Class<T> type);
    
    
    ObjectAppenderSupplier DEFAULT = new ObjectAppenderSupplier() {
        @Override
        public <T> ObjectAppender<T> get(final Class<T> type) {
            return (ObjectAppender<T>) ObjectAppender.TOSTRING_APPENDER;
        }
    };
    
}
