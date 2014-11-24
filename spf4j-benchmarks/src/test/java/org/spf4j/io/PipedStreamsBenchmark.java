
package org.spf4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Strings;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 1)
public class PipedStreamsBenchmark {
   
    private static final String testStr = "asfsdfhjgsdjhfgsjhdgfjhsdgfjhgsdjhfgjsdhgkjfsdkhf34hfHGHDG"
           + "SFDGHJJIU&^%ERSDFGVNHKJU&^%!#@#$%^&*()OJHGCXFDGHJUYTRWERTGFHHJYREWRDFGHJUYTredscxvbbhuytdsdfbvnmjhgfd"
           + "dkjhfkjsdhfkdskgfskjdhfjkdfghsdkjhfglskdfhjgkldfhgksjdfhgklhsdfkghklsfdhgkdfhlkfghfslkdjhgklsdhkghs";
    
    @Benchmark
    public void testSpf4Pipe() throws IOException {
        testSpf(testStr, 64);
    }
    
    @Benchmark
    public void testjavaPipe() throws IOException {
        testJdk(testStr, 64);
    }
    
    
    
    private void testSpf(final String testStr, final int buffSize) throws IOException {
        final PipedOutputStream pos = new PipedOutputStream(buffSize);
        final InputStream pis = pos.getInputStream();
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
    
    private void testJdk(final String testStr, final int buffSize) throws IOException {
        final java.io.PipedOutputStream pos = new java.io.PipedOutputStream();
        final java.io.PipedInputStream pis = new java.io.PipedInputStream(pos, buffSize);
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

    
    
}
