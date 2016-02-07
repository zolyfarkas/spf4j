
package org.spf4j.io.tcp.proxy;

import java.nio.ByteBuffer;

/**
 *
 * @author zoly
 */
public interface Sniffer {

    void received(ByteBuffer data);

}
