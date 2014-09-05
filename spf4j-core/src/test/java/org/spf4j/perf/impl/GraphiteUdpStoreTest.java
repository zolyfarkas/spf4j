 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spf4j.perf.impl;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.recyclable.ObjectCreationException;

/**
 *
 * @author zoly
 */
public final class GraphiteUdpStoreTest {


    @Test
    public void testGraphiteUdpStore() throws IOException, ObjectCreationException {

        final GraphiteUdpStore store = new GraphiteUdpStore("127.0.0.1", 1976);

        DatagramChannel channel = DatagramChannel.open();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 1976);
        channel.socket().bind(inetSocketAddress);

        DefaultScheduler.INSTANCE.schedule(new AbstractRunnable(true) {

            @Override
            public void doRun() throws Exception {
                store.saveMeasurements(new EntityMeasurementsInfoImpl("bla", "ms",
                        new String[]{"val1", "val2", "val3"},
                        new String[]{"ms", "ms", "ms"}),
                        System.currentTimeMillis(), 0, 1L, 2L, 3L);
                System.out.println("measurements sent");
            }
        }, 5, TimeUnit.SECONDS);
        ByteBuffer bb = ByteBuffer.allocate(512);
        channel.receive(bb);
        byte [] rba = new byte [bb.position()];
        bb.flip();
        bb.get(rba);
        System.out.println("Received = " + new String(rba, Charsets.UTF_8));
    }

}
