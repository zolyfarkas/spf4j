
package org.spf4j.perf.impl.chart;

import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
public class ChartsTest {


    @Test
    public void testGapsFiller1() {
        Pair<long[], double[][]> fillGaps = Charts.fillGaps(new long[] {0L}, new long [] [] {{1L, 2L, 3L}},
                10, 3);
        Assert.assertEquals(0L, fillGaps.getFirst()[0]);
        Assert.assertEquals(3d, fillGaps.getSecond()[0][2]);
    }

    @Test
    public void testGapsFiller2() {
        Pair<long[], double[][]> fillGaps = Charts.fillGaps(new long[] {0L, 30L},
                new long [] [] {{1L, 2L, 3L}, {10L, 20L, 30L}},
                10, 3);
        Assert.assertEquals(4, fillGaps.getFirst().length);
        Assert.assertEquals(4, fillGaps.getSecond().length);
        Assert.assertEquals(10L, fillGaps.getFirst()[1]);
        Assert.assertEquals(20L, fillGaps.getFirst()[2]);
        Assert.assertEquals(Double.NaN, fillGaps.getSecond()[1][2]);
        Assert.assertEquals(Double.NaN, fillGaps.getSecond()[2][2]);
    }

}
