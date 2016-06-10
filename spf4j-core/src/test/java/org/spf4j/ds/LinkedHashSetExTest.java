
package org.spf4j.ds;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author zoly
 */
public final class LinkedHashSetExTest {


    @Test
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void testSomeMethod() {
        LinkedSet<Integer> map = new LinkedHashSetEx(2);
        map.add(10);
        map.add(1);
        map.add(100);
        map.add(2);
        map.add(3);
        map.remove(100);
        assertEquals(10, map.iterator().next().intValue());
        assertEquals(3, map.getLastValue().intValue());
        assertEquals(3, map.pollLastValue().intValue());
        assertEquals(2, map.pollLastValue().intValue());
        assertEquals(1, map.pollLastValue().intValue());
        assertEquals(10, map.pollLastValue().intValue());
        assertNull(map.pollLastValue());

    }

}
