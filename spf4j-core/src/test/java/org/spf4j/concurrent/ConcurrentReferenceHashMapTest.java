
package org.spf4j.concurrent;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ConcurrentReferenceHashMapTest {


    @Test
    public void testSomeMethod() {
        ConcurrentReferenceHashMap<String, String> map = new ConcurrentReferenceHashMap(
                ConcurrentReferenceHashMap.ReferenceType.STRONG, ConcurrentReferenceHashMap.ReferenceType.STRONG);
        for (int i = 0; i < 10000; i++) {
            String key = "k" + i;
            String value = "v" + i;
            map.put(key, value);
            Assert.assertEquals(value, map.get(key));
        }
        int size = map.size();
        int count = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            count++;
        }
        Assert.assertEquals(size, count);

    }

}
