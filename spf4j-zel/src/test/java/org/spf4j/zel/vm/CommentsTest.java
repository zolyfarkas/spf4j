
package org.spf4j.zel.vm;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class CommentsTest {


    @Test
    public void testEx() throws CompileException, ExecutionException, InterruptedException, IOException {
        String qsort = Resources.toString(Resources.getResource(CommentsTest.class, "comments.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort);
        System.out.println(p);
        p.execute();
    }


}
