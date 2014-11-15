package org.spf4j.stackmonitor;

import org.junit.Test;
import org.spf4j.base.IntMath;

/**
 *
 * @author zoly
 */
public class SamplerTest {
    


    @Test
    public void testSamplingTimes() {
        IntMath.XorShift32 random = new IntMath.XorShift32();
        for (int i = 0; i < 100; i++) {
            System.out.println(random.nextInt() % 10);
        }
        
    }
    
}
