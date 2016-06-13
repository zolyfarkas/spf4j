
package org.spf4j.zel.vm;

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
