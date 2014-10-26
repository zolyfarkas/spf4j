 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.concurrent;

import java.util.UUID;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class UIDGeneratorTest {

    @Test
    public void testSomeMethod() {
        UIDGenerator idGen = new UIDGenerator(new ScalableSequence(0, 50), System.currentTimeMillis());
        long startTime  = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            System.out.println("ID: " + idGen.next());
        }
        long sw1 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            System.out.println("ID: " + UUID.randomUUID().toString());
        }
        long end = System.currentTimeMillis();
        System.out.println("Fast = " + (sw1 - startTime));
        System.out.println("Slow = " + (end - sw1));
    }
    
}
