
package org.spf4j.io;

import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author zoly
 * @param <T> - type of object to append.
 */
@ParametersAreNonnullByDefault
public interface ObjectAppender<T> {

    void append(T object, Appendable appendTo) throws IOException;
    
    
    ObjectAppender<Object> TOSTRING_APPENDER = new ObjectAppender<Object>() {
        @Override
        public void append(final Object object, final Appendable appendTo) throws IOException {
            appendTo.append(object.toString());
        }
    };
    
}
