
package org.spf4j.ds;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.PriorityQueue;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.MutableHolder;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public final class UpdateablePriorityQueueTest {


    @Test
    public void test1() {
        UpdateablePriorityQueue<Integer> queue = new UpdateablePriorityQueue<>(4);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.add(4);
        Assert.assertEquals(Integer.valueOf(1), queue.poll());
        Assert.assertNotEquals(Integer.valueOf(4), queue.poll());
    }

    @Test
    public void test1p() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(4);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.add(4);
        Assert.assertEquals(Integer.valueOf(1), queue.poll());
        Assert.assertNotEquals(Integer.valueOf(4), queue.poll());
    }

    @Test
    public void test2() {
        UpdateablePriorityQueue<Integer> queue = new UpdateablePriorityQueue<>(0);
        queue.add(4);
        queue.add(3);
        queue.add(2);
        queue.add(1);
        Assert.assertEquals(Integer.valueOf(1), queue.poll());
        Assert.assertNotEquals(Integer.valueOf(4), queue.poll());
    }


    @Test
    public void test3() {
        UpdateablePriorityQueue<Integer> queue = new UpdateablePriorityQueue<>(0);
        queue.add(1);
        queue.add(4);
        queue.add(2);
        queue.add(3);
        Assert.assertEquals(Integer.valueOf(1), queue.poll());
        Assert.assertNotEquals(Integer.valueOf(4), queue.poll());
    }

    @Test
    public void test4() {
        UpdateablePriorityQueue<Integer> queue = new UpdateablePriorityQueue<>(0);
        UpdateablePriorityQueue.ElementRef add = queue.add(1);
        queue.add(4);
        queue.add(2);
        queue.add(3);
        add.setElem(5);
        Assert.assertEquals(Integer.valueOf(2), queue.poll());
        Assert.assertEquals(3, queue.size());
        add.setElem(1);
        queue.add(10);
        Assert.assertEquals(Integer.valueOf(1), queue.poll());
        Assert.assertEquals(3, queue.size());
        queue.add(0);
        Assert.assertEquals(Integer.valueOf(0), queue.poll());
        Assert.assertEquals(3, queue.size());
        Assert.assertNotEquals(Integer.valueOf(4), queue.poll());
    }

    @Test
    public void test5() {
        UpdateablePriorityQueue<Integer> queue = new UpdateablePriorityQueue<>(0);
        queue.add(1);
        UpdateablePriorityQueue.ElementRef add = queue.add(4);
        queue.add(2);
        queue.add(3);
        add.setElem(0);
        Assert.assertEquals(Integer.valueOf(0), queue.poll());
        Assert.assertEquals(3, queue.size());
    }


    @Test
    public void test6() {
        UpdateablePriorityQueue<MutableHolder<Integer>> queue = new UpdateablePriorityQueue<>(0);
        queue.add(new MutableHolder<>(3));
        queue.add(new MutableHolder<>(2));
        queue.add(new MutableHolder<>(4));
        UpdateablePriorityQueue<MutableHolder<Integer>>.ElementRef add = queue.add(new MutableHolder<>(1));
        UpdateablePriorityQueue<MutableHolder<Integer>>.ElementRef add1 = queue.add(new MutableHolder<>(5));
        Assert.assertEquals(Integer.valueOf(1), queue.peek().getValue());
        add.getElem().setValue(10);
        add.elementMutated();
        Assert.assertEquals(Integer.valueOf(2), queue.peek().getValue());
        add1.getElem().setValue(0);
        add1.elementMutated();
        Assert.assertEquals(Integer.valueOf(0), queue.peek().getValue());
        add1.remove();
        Assert.assertEquals(Integer.valueOf(2), queue.peek().getValue());
    }

    @Test
    public void testRemove() {
        UpdateablePriorityQueue<MutableHolder<Integer>> queue = new UpdateablePriorityQueue<>(0);
        final MutableHolder<Integer> value = new MutableHolder<>(3);
        UpdateablePriorityQueue.ElementRef ref = queue.add(value);
        final MutableHolder<Integer> four = new MutableHolder<>(4);
        queue.add(four);
        final MutableHolder<Integer> five = new MutableHolder<>(5);
        queue.add(five);
        final MutableHolder<Integer> six = new MutableHolder<>(6);
        queue.add(six);
        MutableHolder<Integer> poll = queue.poll();
        Assert.assertEquals(value, poll);
        Assert.assertFalse(ref.remove());
        Iterator<MutableHolder<Integer>> iterator = queue.iterator();
        iterator.next();
        iterator.next();
        iterator.remove();
        iterator.next();
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(2, queue.size());
    }

}
