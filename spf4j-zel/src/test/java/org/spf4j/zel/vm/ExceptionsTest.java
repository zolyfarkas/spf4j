
package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Throwables;

/**
 *
 * @author zoly
 */
public final class ExceptionsTest {


    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void testEx() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("throw 3");
       try {
        p.execute();
        Assert.fail();
       } catch (ZExecutionException ex) {
           Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
       }
    }

    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void testEx2() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("a = func { throw 5 };\n b = 1 + 1;\n a();\n return b ");
       try {
        p.execute();
        Assert.fail();
       } catch (ZExecutionException ex) {
           Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
       }
    }


}
