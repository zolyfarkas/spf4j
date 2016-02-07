package org.spf4j.io.tcp.proxy;

import java.nio.channels.SocketChannel;

/**
 *
 * @author zoly
 */
public interface SnifferFactory {

    Sniffer get(SocketChannel channel);

}
