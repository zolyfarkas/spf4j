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
package org.spf4j.base;

import junit.framework.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author zoly
 */
public final class StringsTest {

    public StringsTest() {
    }

    @Test
    public void testDistance() {
        assertEquals(3, Strings.distance("abc", "abcdef"));
        assertEquals(3, Strings.distance("def", "abcdef"));
        assertEquals(1, Strings.distance("abc", "bc"));
        assertEquals(3, Strings.distance("abc", "def"));
        assertEquals(1, Strings.distance("zoltran", "zoltan"));
    }
    
    @Test
    public void testEscaping() {
        String res = Strings.unescape("a\\n");
        Assert.assertEquals("a\n", res);
    }
    
    @Test
    public void testUnsafeOps() {
       String testString = "dfjgdjshfgsjdhgfskhdfkdshf\ndfs@#$%^&\u63A5\u53D7*($%^&*()(*&^%$#@!>::><>?{PLKJHGFDEWSDFG";
       char [] chars = Strings.steal(testString);
       String otherString = Strings.wrap(chars);
       Assert.assertEquals(testString, otherString);
       
       byte [] bytes = Strings.toUtf8(testString);
       otherString = Strings.fromUtf8(bytes);
       Assert.assertEquals(testString, otherString);
              
    }
    

}
