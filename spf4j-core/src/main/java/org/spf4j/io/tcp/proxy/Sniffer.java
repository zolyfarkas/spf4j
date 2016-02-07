
package org.spf4j.io.tcp.proxy;

import java.nio.ByteBuffer;

/**
 *
 * @author zoly
 */
public interface Sniffer {

    /**
     * Invoked on data receive/transmission.
     * @param data - the byte buffer containing the data.
     * @param nrBytes - nr bytes in the buffer. The data in the buffer is from position-nrBytes -> position.
     * nrBytes will be -1 on EOF.
     */
    void received(ByteBuffer data, int nrBytes);

}
