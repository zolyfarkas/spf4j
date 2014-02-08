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
package org.spf4j.zel.vm;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;


/**
 * the memory implementation of the ZEL vm.
 *
 * @author zoly
 */
public final class Memory extends HashMap {

    /**
     * get a value from memory
     */
    public Object getValue(final String name)
            throws CompileException, ZExecutionException, InterruptedException, ExecutionException {
        return Program.getValue(this, name);
    }

    /**
     * add value to the memory (uses vm)
     *
     * @param name String
     * @param value String
     * @throws Exception
     */
    public void addValue(final String name, final Object value)
            throws CompileException, ZExecutionException, InterruptedException, ExecutionException {
        Program.addValue(this, name, value);
    }

    /**
     * dump the core for this memory
     */
    public String dumpCore() {
        return Program.dumpCore("[root]", this, 1, -1);
    }

    /**
     * dump the core for this maxLevel
     * @param maxLevel
     */
    public String dumpCore(final int maxLevel) {
        return Program.dumpCore("[root]", this, 1, maxLevel + 1);
    }

    /**
     * to string for debug purposes, limited to descend max 2 levels
     */
    @Override
    public String toString() {
        return dumpCore(2);
    }
}
