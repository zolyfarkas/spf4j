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

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.io.ConfigurableAppenderSupplier;
import org.spf4j.io.appenders.LocalDateAppender;
import org.spf4j.ssdump2.avro.AMethod;

/**
 *
 * @author zoly
 */
public class Slf4jMessageFormatterTest {

    @Test
    public void testFormatter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Slf4jMessageFormatter.format(sb, "bla bla");
        Assert.assertEquals("bla bla", sb.toString());
        sb.setLength(0);
        Slf4jMessageFormatter.format(sb, "bla bla {}", "coco");
        Assert.assertEquals("bla bla coco", sb.toString());
        sb.setLength(0);
        Slf4jMessageFormatter.format(sb, "\\{}bla bla {}", "coco");
        Assert.assertEquals("{}bla bla coco", sb.toString());             
    }
    
    @Test
    public void testFormatter2() throws IOException {
        ConfigurableAppenderSupplier appSupp = new ConfigurableAppenderSupplier();
        System.out.println(appSupp);
        StringBuilder sb = new StringBuilder();
        final long currentTimeMillis = System.currentTimeMillis();
        Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp, new java.sql.Date(currentTimeMillis));
        Assert.assertEquals("bla bla " + LocalDateAppender.FMT.print(currentTimeMillis), sb.toString());
        sb.setLength(0);
        AMethod method = AMethod.newBuilder().setName("m1").setDeclaringClass("c1").build();
        int written = Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp, method);
        Assert.assertEquals("bla bla {\"declaringClass\":\"c1\",\"name\":\"m1\"}", sb.toString());
        Assert.assertEquals(1, written);
        sb.setLength(0);
        written = Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp, method, "yohooo");
        System.out.println("formatted message: " + sb.toString());
        Assert.assertEquals(1, written);
        sb.setLength(0);
        written = Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp);
        System.out.println("formatted message: " + sb.toString());
        Assert.assertEquals(0, written);
        sb.setLength(0);
        EscapeJsonStringAppendableWrapper escaper = new EscapeJsonStringAppendableWrapper(sb);
        Slf4jMessageFormatter.format(escaper, "bla bla {} {}", appSupp, "\n\u2013\u0010",
                new int [] {1, 2, 3});
        System.out.println("formatted message: " + sb.toString());        
        Assert.assertEquals("bla bla \\n–\\u0010 [1, 2, 3]", sb.toString());
    }    
    
    
    
}
