package org.spf4j.base;

import org.junit.Assert;
import org.junit.Test;

public final class IntMathTest {

    public IntMathTest() {
    }

    @Test
    public void testRandom() {
        IntMath.XorShift32 random = new IntMath.XorShift32();
        int sum = 0;
        for (int i = 1; i < 100; i++) {
            sum += Math.abs(random.nextInt()) % 100;
        }
        sum /= 100;
        Assert.assertTrue(sum > 15);

        IntMath.XorShift128 random2 = new IntMath.XorShift128();
        sum = 0;
        for (int i = 1; i < 100; i++) {
            sum += Math.abs(random2.nextInt()) % 100;
        }
        sum /= 100;
        Assert.assertTrue(sum > 15);
    }


    public void testPowerOf2Methods() {
        int power = IntMath.closestPowerOf2(8000);
        int value = 1 << power;
        Assert.assertEquals(8192, value);
        Assert.assertEquals(8192, IntMath.closestPowerOf2Number(8000));
        Assert.assertEquals(8192, IntMath.closestPowerOf2Number(8192));
    }

}
