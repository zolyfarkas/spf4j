
package org.spf4j.zel.vm;

import java.util.concurrent.ExecutionException;
import org.junit.Test;

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
       } catch (ZExecutionException ex) {
           ex.printStackTrace();
       }
       //Assert.assertEquals(2, result.intValue());
    }

    @Test
    public void testEx2() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("a = func { throw 5 };\n b = 1 + 1;\n a();\n return b ");
       try {
        p.execute();
       } catch (ZExecutionException ex) {
           ex.printStackTrace();
       }
       //Assert.assertEquals(2, result.intValue());
    }


}
