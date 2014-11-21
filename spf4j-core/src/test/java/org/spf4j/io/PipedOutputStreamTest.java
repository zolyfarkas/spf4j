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
    
    public PipedOutputStreamTest() {
    }

    @Test
    public void testStreamPiping() throws IOException {
        test("This is a super cool, mega dupper test string for testing piping..........E");
        StringBuilder sb = new StringBuilder();
        final IntMath.XorShift32 random = new IntMath.XorShift32();
        int nrChars = Math.abs(random.nextInt() % 100000);
        for (int i = 0; i < nrChars; i++) {
            sb.append((char) Math.abs(random.nextInt() % 128));
        }
        test(sb.toString());
    }

    private void test(final String testStr) throws IOException {
        final PipedOutputStream pos = new PipedOutputStream(8);
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
        try (InputStream is = pos.getInputStream()) {
            byte [] buffer  = new byte[1024];
            int read;
            while((read = is.read(buffer)) > 0) {
                sb.append(Strings.fromUtf8(buffer, 0, read));
            }
        }
        Assert.assertEquals(testStr, sb.toString());
    }
    
}
