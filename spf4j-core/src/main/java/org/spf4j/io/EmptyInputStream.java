
package org.spf4j.io;

import java.io.InputStream;


public final class EmptyInputStream extends InputStream {

    private EmptyInputStream() { }

    @Override
    public int read() {
        return -1;
    }

    public static final InputStream EMPTY = new EmptyInputStream();

}
