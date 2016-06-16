/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import java.util.List;
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class PairTest {

    @Test
    public void testToList() {
        Pair<String, String> pair1 = Pair.of("5", ", adf\"klf ");
        Pair<String, String> pair2 = Pair.from(pair1.toString());
        Assert.assertEquals(pair1, pair2);
    }

    @Test
    public void testToMap() {
        Pair<String, String> pair1 = Pair.of("5", ", adf\"klf ");
        Pair<String, LocalDate> pair2 = Pair.of("bla", new LocalDate());
        Assert.assertEquals(2, Pair.asMap(pair1, pair2).size());
    }

    @Test
    public void testNull() {
        Pair<String, String> pair1 = Pair.of(null, null);
        Assert.assertEquals("(,)", pair1.toString());
         List<Object> toList = pair1.toList();
        Assert.assertNull(toList.get(0));
        Assert.assertNull(toList.get(1));
    }

}