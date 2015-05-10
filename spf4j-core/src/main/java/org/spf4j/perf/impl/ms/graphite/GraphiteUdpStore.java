package org.spf4j.perf.impl.ms.graphite;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import org.spf4j.base.Handler;
import org.spf4j.base.Strings;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.impl.ms.Id2Info;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Template;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;

/**
 *
 * @author zoly
 */
public final class GraphiteUdpStore implements MeasurementStore {

    @Override
    public void flush() {
        // No buffering yet
    }

    private static class DatagramChannelSupplierFactory implements RecyclingSupplier.Factory<DatagramChannel> {

        private final InetSocketAddress address;

        public DatagramChannelSupplierFactory(final InetSocketAddress address) {
            this.address = address;
        }

        @Override
        public DatagramChannel create() throws ObjectCreationException {
            DatagramChannel datagramChannel;
            try {
                datagramChannel = DatagramChannel.open();
            } catch (IOException ex) {
                throw new ObjectCreationException(ex);
            }
            try {
                datagramChannel.connect(address);
                return datagramChannel;
            } catch (IOException ex) {
                try {
                    datagramChannel.close();
                } catch (IOException ex1) {
                    ex1.addSuppressed(ex);
                    throw new ObjectCreationException(ex1);
                }
                throw new ObjectCreationException(ex);
            }
        }

        @Override
        public void dispose(final DatagramChannel object) throws ObjectDisposeException {
            try {
                object.close();
            } catch (IOException ex) {
                throw new ObjectDisposeException(ex);
            }
        }

        @Override
        public boolean validate(final DatagramChannel object, final Exception e) throws Exception {
            return e == null || !(Throwables.getRootCause(e) instanceof IOException);
        }

    }

    private final RecyclingSupplier<DatagramChannel> datagramChannelSupplier;

    private final InetSocketAddress address;

    public static final int MAX_UDP_MSG_SIZE = 512;

   public GraphiteUdpStore(final String hostPort) throws ObjectCreationException, URISyntaxException {
        this(new URI("graphiteUdp://" + hostPort));
    }

    public GraphiteUdpStore(final URI uri) throws ObjectCreationException {
        this(uri.getHost(), uri.getPort());
    }

    public GraphiteUdpStore(final String hostName, final int port) throws ObjectCreationException {
        address = new InetSocketAddress(hostName, port);
        datagramChannelSupplier = new RecyclingSupplierBuilder<DatagramChannel>(1,
                new DatagramChannelSupplierFactory(address)).build();
    }

    @Override
    public long alocateMeasurements(final MeasurementsInfo measurement, final int sampleTimeMillis) {
        return Id2Info.getId(measurement);
    }

    @Override
    public void saveMeasurements(final long tableId,
            final long timeStampMillis, final long... measurements) throws IOException {

        try {
            Template.doOnSupplied(new HandlerImpl(measurements, Id2Info.getInfo(tableId), timeStampMillis),
                    datagramChannelSupplier, 3, 1000, 60000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void writeMetric(final MeasurementsInfo measurementInfo, final String measurementName,
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
        return "GraphiteUdpStore{address=" + address + '}';
    }

    @Override
    public void close() throws IOException {
        try {
            datagramChannelSupplier.dispose();
        } catch (ObjectDisposeException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class HandlerImpl implements Handler<DatagramChannel, IOException> {

        private final long[] measurements;
        private final MeasurementsInfo measurementInfo;
        private final long timeStampMillis;

        public HandlerImpl(final long[] measurements, final MeasurementsInfo measurementInfo,
                final long timeStampMillis) {
            this.measurements = measurements;
            this.measurementInfo = measurementInfo;
            this.timeStampMillis = timeStampMillis;
        }

        @Override
        public void handle(final DatagramChannel datagramChannel, final long deadline) throws IOException {
            try (ByteArrayBuilder bos = new ByteArrayBuilder();
            OutputStreamWriter os = new OutputStreamWriter(bos, Charsets.UTF_8)) {

                int msgStart = 0, msgEnd = 0, prevEnd = 0;

                for (int i = 0; i < measurements.length; i++) {
                    writeMetric(measurementInfo, measurementInfo.getMeasurementName(i),
                            measurements[i], timeStampMillis, os);
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
        }
    }

}
