package org.spf4j.base;

import org.junit.Test;

public final class IntMathTest {

    public IntMathTest() {
    }

    @Test
    public void testRandom() {
        IntMath.XorShift32 random = new IntMath.XorShift32();
        for (int i = 1; i < 100; i++) {
            System.out.println("rand " + i + " = " + random.nextInt() % 100);
        }

        IntMath.XorShift128 random2 = new IntMath.XorShift128();
        for (int i = 1; i < 100; i++) {
            System.out.println("rand " + i + " = " + random2.nextInt() % 100);
        }
    }

}
