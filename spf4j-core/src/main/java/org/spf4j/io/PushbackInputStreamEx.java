
package org.spf4j.io;

import com.google.common.io.BaseEncoding;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import java.io.InputStream;


/**
 *
 * @author zoly
 */
@CleanupObligation
public final class PushbackInputStreamEx  extends java.io.PushbackInputStream {

    public PushbackInputStreamEx(final InputStream in, final int size) {
        super(in, size);
    }

    public PushbackInputStreamEx(final InputStream in) {
        super(in);
    }

    @Override
    public String toString() {
        return "PushbackInputStream{"
                + ((buf == null) ? "buf=null" : "buf=" + BaseEncoding.base64().encode(buf))
                + ", pos=" + pos
                + ", wrapped=" + this.in + '}';
    }

    public InputStream getUnderlyingStream() {
        return this.in;
    }

}
