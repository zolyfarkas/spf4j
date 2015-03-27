package org.spf4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.IntMath;
import org.spf4j.base.Strings;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
public class PipedOutputStreamTest {


    @Test
    public void testStreamPiping() throws IOException {
        test("This is a super cool, mega dupper test string for testing piping..........E", 8, false);
        final IntMath.XorShift32 random = new IntMath.XorShift32();
        int nrChars = Math.abs(random.nextInt() % 100000);
        StringBuilder sb = generateTestStr(nrChars);
        test(sb.toString(), Math.abs(random.nextInt() % 10000), false);
    }

    public static StringBuilder generateTestStr(int nrChars) {
        final IntMath.XorShift32 random = new IntMath.XorShift32();
        StringBuilder sb = new StringBuilder(nrChars);
        for (int i = 0; i < nrChars; i++) {
            sb.append((char) Math.abs(random.nextInt() % 127));
        }
        return sb;
    }

    public static void test(final String testStr, final int buffSize, final boolean buffered) throws IOException {
        final PipedOutputStream pos = new PipedOutputStream(buffSize);
        final InputStream pis;
        if (buffered) {
            pis = new MemorizingBufferedInputStream(pos.getInputStream());
        } else {
            pis = pos.getInputStream();
        }

        DefaultExecutor.INSTANCE.execute(new AbstractRunnable() {

            @Override
            public void doRun() throws Exception {
                try (OutputStream os = pos) {
                    final byte[] utf8 = Strings.toUtf8(testStr);
                    os.write(utf8[0]);
                    os.write(utf8, 1, 10);
                    os.write(utf8, 11, utf8.length - 11);
                }
            }
        });
        StringBuilder sb = new StringBuilder();
        try (InputStream is = pis) {
            byte [] buffer  = new byte[1024];
            int read;
            while((read = is.read(buffer)) > 0) {
                sb.append(Strings.fromUtf8(buffer, 0, read));
            }
        }
        Assert.assertEquals(testStr, sb.toString());
    }

    @Test(expected = IOException.class)
    public void testNoReaderBehaviour() throws IOException {
        final PipedOutputStream pos = new PipedOutputStream(1024);
        pos.write(123);
    }

    @Test(expected = IOException.class)
    public void testNoReaderBehaviour2() throws IOException {
        final PipedOutputStream pos = new PipedOutputStream(1024);
        try (InputStream is = pos.getInputStream()) {
            pos.write(123);
            pos.flush();
            int val = is.read();
            Assert.assertEquals(123, val);
        }
        pos.write(123);
    }


}
