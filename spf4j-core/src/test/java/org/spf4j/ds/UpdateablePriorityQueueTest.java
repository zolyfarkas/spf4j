/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        queue.add(MutableHolder.of(3));
        queue.add(MutableHolder.of(2));
        queue.add(MutableHolder.of(4));
        UpdateablePriorityQueue<MutableHolder<Integer>>.ElementRef add = queue.add(MutableHolder.of(1));
        UpdateablePriorityQueue<MutableHolder<Integer>>.ElementRef add1 = queue.add(MutableHolder.of(5));
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
        final MutableHolder<Integer> value = MutableHolder.of(3);
        UpdateablePriorityQueue.ElementRef ref = queue.add(value);
        final MutableHolder<Integer> four = MutableHolder.of(4);
        queue.add(four);
        final MutableHolder<Integer> five = MutableHolder.of(5);
        queue.add(five);
        final MutableHolder<Integer> six = MutableHolder.of(6);
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
