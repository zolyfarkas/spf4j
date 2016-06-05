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

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author zoly
 */
public final class StringsTest {

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

    @Test
    public void testSubSequence() {
        String str = "dsfhjgsdjfgwuergfedhgfjhwheriufwiueruhfguyerugfweuyrygfwueyrghfuwoeruhgfdgwsjhfg";
        assertEquals(0, Strings.compareTo(str.subSequence(3, 15), Strings.subSequence(str, 3, 15)));
        Assert.assertTrue(Strings.equals(str.subSequence(15, 15), Strings.subSequence(str, 15, 15)));
        Assert.assertTrue(Strings.equals(str.subSequence(0, str.length()), Strings.subSequence(str, 0, str.length())));
        Assert.assertTrue(str.subSequence(3, 15).toString().equals(Strings.subSequence(str, 3, 15).toString()));
    }


    @Test
    public void testEndsWith() {
        Assert.assertEquals(true, Strings.endsWith("dfjkshfks", "hfks"));
        Assert.assertEquals(true, Strings.endsWith("dfjkshfks", ""));
        Assert.assertEquals(false, Strings.endsWith("dfjkshfks", "hfk"));
        Assert.assertEquals(true, Strings.endsWith("dfjkshfks", "dfjkshfks"));
        Assert.assertEquals(false, Strings.endsWith("dfjkshfks", "dfjkshfksu"));
    }

    @Test
    public void testEncoding() {
        StringBuilder sb = new StringBuilder();
        Strings.appendUnsignedString(sb, 38, 4);
        Assert.assertEquals(Integer.toHexString(38), sb.toString());
        System.out.println("Encoded: " + sb);
    }

    @Test
    public void testEncoding2() {
        StringBuilder sb = new StringBuilder();
        Strings.appendUnsignedStringPadded(sb, 38, 5, 4);
        Assert.assertEquals(4, sb.length());
        System.out.println("Encoded: " + sb);
    }


    @Test
    public void testJsonEncoding() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        Strings.escapeJsonString("\n\u0008\u0000abc\"", sb);
        sb.append("\"");
        System.out.println("Encoded: " + sb);
        Assert.assertEquals("\"\\n\\b\\u0000abc\\\"\"", sb.toString());
    }



}
