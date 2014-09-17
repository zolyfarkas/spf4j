package org.spf4j.perf.impl.ms.graphite;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import javax.net.SocketFactory;
import org.spf4j.base.Handler;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import static org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore.writeMetric;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Template;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;

/**
 *
 * @author zoly
 */
public final class GraphiteTcpStore implements MeasurementStore {

    private static class WriterSupplierFactory implements RecyclingSupplier.Factory<Writer> {

        private final String hostName;
        private final int port;
        private final SocketFactory socketFactory;

        public WriterSupplierFactory(final SocketFactory socketFactory, final String hostName, final int port) {
            this.hostName = hostName;
            this.port = port;
            this.socketFactory = socketFactory;
        }

        @Override
        public Writer create() throws ObjectCreationException {
            Socket socket;
            try {
                socket = socketFactory.createSocket(hostName, port);
                return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8));
            } catch (IOException ex) {
                throw new ObjectCreationException(ex);
            }
        }

        @Override
        public void dispose(final Writer object) throws ObjectDisposeException {
            try {
                object.close();
            } catch (IOException ex) {
                throw new ObjectDisposeException(ex);
            }
        }

        @Override
        public boolean validate(final Writer object, final Exception e) {
            return e == null || !(Throwables.getRootCause(e) instanceof IOException);
        }

    }

    private final RecyclingSupplier<Writer> socketWriterSupplier;

    private final InetSocketAddress address;

    public GraphiteTcpStore(final String hostPort) throws ObjectCreationException, URISyntaxException {
        this(new URI("graphiteTcp://" + hostPort));
    }
    
    public GraphiteTcpStore(final URI uri) throws ObjectCreationException {
        this(uri.getHost(), uri.getPort());
    }
    
    public GraphiteTcpStore(final String hostName, final int port) throws ObjectCreationException {
        this(hostName, port, SocketFactory.getDefault());
    }

    public GraphiteTcpStore(final String hostName, final int port, final SocketFactory socketFactory)
            throws ObjectCreationException {
        address = new InetSocketAddress(hostName, port);
        socketWriterSupplier = new RecyclingSupplierBuilder<Writer>(1,
                new WriterSupplierFactory(socketFactory, hostName, port)).build();
    }

    @Override
    public void alocateMeasurements(final EntityMeasurementsInfo measurement, final int sampleTimeMillis) {
        // DO NOTHING.
    }

    @Override
    public void saveMeasurements(final EntityMeasurementsInfo measurementInfo,
            final long timeStampMillis, final int sampleTimeMillis, final long... measurements) throws IOException {

        try {
            Template.doOnSupplied(new HandlerImpl(measurements, measurementInfo, timeStampMillis),
                    socketWriterSupplier, 3, 1000, 60000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public String toString() {
        return "GraphiteTcpStore{address=" + address + '}';
    }

    @Override
    public void close() throws IOException {
        try {
            socketWriterSupplier.dispose();
        } catch (ObjectDisposeException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class HandlerImpl implements Handler<Writer, IOException> {

        private final long[] measurements;
        private final EntityMeasurementsInfo measurementInfo;
        private final long timeStampMillis;

        public HandlerImpl(final long[] measurements, final EntityMeasurementsInfo measurementInfo,
                final long timeStampMillis) {
            this.measurements = measurements;
            this.measurementInfo = measurementInfo;
            this.timeStampMillis = timeStampMillis;
        }

        @Override
        public void handle(final Writer socketWriter, final long deadline) throws IOException {
            for (int i = 0; i < measurements.length; i++) {
                writeMetric(measurementInfo, measurementInfo.getMeasurementName(i),
                        measurements[i], timeStampMillis, socketWriter);
            }
            socketWriter.flush();
        }
    }

    @Override
    public void flush() {
        // No buffering yet
    }
    
}
