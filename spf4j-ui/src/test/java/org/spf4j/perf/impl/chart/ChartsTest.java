package org.spf4j.perf.impl.chart;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
public class ChartsTest {

  @Test
  public void testGapsFiller1() {
    Pair<long[], double[][]> fillGaps = Charts.fillGaps(new long[]{0L}, new long[][]{{1L, 2L, 3L}},
            10, 3);
    Assert.assertEquals(0L, fillGaps.getFirst()[0]);
    Assert.assertEquals(3d, fillGaps.getSecond()[0][2], 0.01);
  }

  @Test
  public void testGapsFiller2() {
    Pair<long[], double[][]> fillGaps = Charts.fillGaps(new long[]{0L, 30L},
            new long[][]{{1L, 2L, 3L}, {10L, 20L, 30L}},
            10, 3);
    long[] first = fillGaps.getFirst();
    Assert.assertEquals(4, first.length);
    double[][] second = fillGaps.getSecond();
    Assert.assertEquals(4, second.length);
    Assert.assertEquals(10L, first[1]);
    Assert.assertEquals(20L, first[2]);
    Assert.assertEquals(Double.NaN, second[1][2], 0.01);
    Assert.assertEquals(Double.NaN, second[2][2], 0.01);
  }

}
