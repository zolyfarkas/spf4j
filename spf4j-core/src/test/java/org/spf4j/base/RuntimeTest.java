
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
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class RuntimeTest {

    public RuntimeTest() {
    }

    /**
     * Test of goDownWithError method, of class Runtime.
     */
    @Test
    public void testGoDownWithError() throws IOException, InterruptedException, ExecutionException {
        System.out.println("PID=" + Runtime.PID);
        System.out.println("OSNAME=" + Runtime.OS_NAME);
        System.out.println("NR_OPEN_FILES=" + Runtime.getNrOpenFiles());
        System.out.println("LSOF_OUT=" + Runtime.getLsofOutput());
        System.out.println("MAX_OPEN_FILES=" + Runtime.Ulimit.MAX_NR_OPENFILES);
    }
}