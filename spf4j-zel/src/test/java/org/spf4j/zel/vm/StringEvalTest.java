
package org.spf4j.zel.vm;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 * Unit test for next feature implementation.
 * @author zoly
 */
public final class StringEvalTest {


    @Test(expected=CompileException.class)
    public void testEscaping() throws CompileException, ExecutionException, InterruptedException, IOException {
        String qsort = Resources.toString(Resources.getResource(StringEvalTest.class, "stringEscaping.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort);
        System.out.println(p);
        p.execute();
    }


    @Test(expected=CompileException.class)
    public void testEscapingSimple() throws CompileException, ExecutionException, InterruptedException  {
        String qsort = "\" val \\{a} and \\{b} \"";
        Program p = Program.compile(qsort, "a", "b");
        System.out.println(p);
        p.execute(1 , "bla");
    }

}
