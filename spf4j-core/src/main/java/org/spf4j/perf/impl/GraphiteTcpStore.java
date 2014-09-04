package org.spf4j.perf.impl;

import com.google.common.base.Charsets;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.SocketFactory;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import static org.spf4j.perf.impl.GraphiteUdpStore.writeMetric;

/**
 *
 * @author zoly
 */
public final class GraphiteTcpStore implements MeasurementStore {

    private final Writer socketWriter;

    private final InetSocketAddress address;
    
    private final Socket socket;
    
    public GraphiteTcpStore(final String hostName, final int port) throws IOException {
        this(hostName, port, SocketFactory.getDefault());
    }
    
    public GraphiteTcpStore(final String hostName, final int port, final SocketFactory socketFactory)
            throws IOException {
        address = new InetSocketAddress(hostName, port);
        socket = socketFactory.createSocket(hostName, port);
        socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8));
    }

    @Override
    public void alocateMeasurements(final EntityMeasurementsInfo measurement, final int sampleTimeMillis) {
        // DO NOTHING.
    }

    @Override
    public void saveMeasurements(final EntityMeasurementsInfo measurementInfo,
           final long timeStampMillis, final int sampleTimeMillis, final long... measurements) throws IOException {

        for (int i = 0; i < measurements.length; i++) {
            writeMetric(measurementInfo, measurementInfo.getMeasurementName(i),
                    measurements[i], sampleTimeMillis, socketWriter);
        }
        socketWriter.flush();
    }

    @Override
    public String toString() {
        return "GraphiteTcpStore{" + "clientChannel=" + socketWriter + ", address=" + address + '}';
    }

    @Override
    public void close() throws IOException {
        try {
            socketWriter.close();
        } finally {
            socket.close();
        }
    }
 
    
    
}
