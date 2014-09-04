package org.spf4j.perf.impl;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import org.spf4j.base.Strings;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;

/**
 *
 * @author zoly
 */
public final class GraphiteUdpStore implements MeasurementStore {

    private final DatagramChannel datagramChannel;

    private final InetSocketAddress address;
    
    public static final int MAX_UDP_MSG_SIZE = 512;

    public GraphiteUdpStore(final String hostName, final int port) throws IOException {
        address = new InetSocketAddress(hostName, port);
        datagramChannel = DatagramChannel.open();
        datagramChannel.connect(address);
    }

    @Override
    public void alocateMeasurements(final EntityMeasurementsInfo measurement, final int sampleTimeMillis) {
        // DO NOTHING.
    }

    @Override
    public void saveMeasurements(final EntityMeasurementsInfo measurementInfo,
           final long timeStampMillis, final int sampleTimeMillis, final long... measurements) throws IOException {

        ByteArrayBuilder bos = new ByteArrayBuilder();
        OutputStreamWriter os = new OutputStreamWriter(bos, Charsets.UTF_8);

        int msgStart = 0, msgEnd = 0, prevEnd = 0;
        
        for (int i = 0; i < measurements.length; i++) {
            writeMetric(measurementInfo, measurementInfo.getMeasurementName(i),
                    measurements[i], sampleTimeMillis, os);
            os.flush();
            msgEnd = bos.size();
            int length = msgEnd - msgStart;
            if (length > MAX_UDP_MSG_SIZE) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(bos.getBuffer(), msgStart, prevEnd - msgStart);
                datagramChannel.write(byteBuffer);
                msgStart = prevEnd;
            }
            prevEnd = msgEnd;
        }
        if (msgEnd > msgStart) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bos.getBuffer(), msgStart, msgEnd - msgStart);
            datagramChannel.write(byteBuffer);
        }

    }

    public static void writeMetric(final EntityMeasurementsInfo measurementInfo, final String measurementName,
            final long measurement, final long timeStampMillis, final Writer os)
            throws IOException {
        Strings.writeReplaceWhitespaces(measurementInfo.getMeasuredEntity().toString(), '-', os);
        os.append('/');
        Strings.writeReplaceWhitespaces(measurementName, '-', os);
        os.append(' ');
        os.append(Long.toString(measurement));
        os.append(' ');
        os.append(Long.toString(timeStampMillis));
        os.append('\n');
    }

    @Override
    public String toString() {
        return "GraphiteUdpStore{" + "datagramChannel=" + datagramChannel + ", address=" + address + '}';
    }

    @Override
    public void close() throws IOException {
        datagramChannel.close();
    }
 
    
    
}
