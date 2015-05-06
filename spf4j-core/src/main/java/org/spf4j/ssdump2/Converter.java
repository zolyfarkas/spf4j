package org.spf4j.ssdump2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.spf4j.base.Handler;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.ssdump2.avro.AMethod;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.ssdump2.avro.ASample;
import org.spf4j.stackmonitor.Method;

/**
 *
 * @author zoly
 */
public final class Converter {

    private Converter() { }

    public static <E extends Exception> int convert(final Method method, final SampleNode node,
            final int parentId, final int id,
            final Handler<ASample, E> handler) throws E {
        ASample sample = new ASample();
        sample.id = id;
        sample.count = node.getSampleCount();
        AMethod m = new AMethod();
        m.setName(method.getMethodName());
        m.setDeclaringClass(method.getDeclaringClass());
        sample.method = m;
        sample.parentId = parentId;
        handler.handle(sample, Long.MAX_VALUE);
        final Map<Method, SampleNode> subNodes = node.getSubNodes();
        int nid = id + 1;
        if (subNodes != null) {
            for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
                nid = convert(entry.getKey(), entry.getValue(), id, nid, handler);
            }
        }
        return nid;
    }

    public static SampleNode convert(final Iterator<ASample> samples) {
        TIntObjectMap<SampleNode> index = new TIntObjectHashMap<>();
        while (samples.hasNext()) {
            ASample asmp = samples.next();
            SampleNode sn = new SampleNode(asmp.count, new HashMap<Method, SampleNode>());
            SampleNode parent = index.get(asmp.parentId);
            if (parent != null) {
                AMethod method = asmp.getMethod();
                Method m = Method.getMethod(method.declaringClass, method.getName());
                final Map<Method, SampleNode> subNodes = parent.getSubNodes();
                if (subNodes == null) {
                    throw new IllegalStateException("Bug, state " + index + "; at node " + asmp);
                }
                subNodes.put(m, sn);
            }
            index.put(asmp.id, sn);
        }
        return index.get(0);
    }

    public static void save(final File file, final SampleNode collected) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(file))) {
            final SpecificDatumWriter<ASample> writer = new SpecificDatumWriter<>(ASample.SCHEMA$);
            final BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(bos, null);
            Converter.convert(Method.ROOT, collected,
                    -1, 0, new Handler<ASample, IOException>() {

                        @Override
                        public void handle(final ASample object, final long deadline)
                                throws IOException {
                            writer.write(object, encoder);
                        }
                    });
            encoder.flush();
        }
    }

    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public static SampleNode load(final File file) throws IOException {
        try (MemorizingBufferedInputStream bis = new MemorizingBufferedInputStream(new FileInputStream(file))) {
            final PushbackInputStream pis = new PushbackInputStream(bis);
            final SpecificDatumReader<ASample> reader = new SpecificDatumReader<>(ASample.SCHEMA$);
            final BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(pis, null);
            return convert(new Iterator<ASample>() {

                @Override
                public boolean hasNext() {
                    try {
                        int read = pis.read();
                        pis.unread(read);
                        return read >= 0;
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                @SuppressFBWarnings
                public ASample next() {
                    try {
                        return reader.read(null, decoder);
                    } catch (IOException ex) {
                        NoSuchElementException e = new NoSuchElementException();
                        e.addSuppressed(ex);
                        throw e;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        }
    }

}
