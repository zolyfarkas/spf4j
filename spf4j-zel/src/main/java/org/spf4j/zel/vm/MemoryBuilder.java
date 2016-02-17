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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.spf4j.base.Pair;


public final  class MemoryBuilder {

    private final ArrayList<Object> memory;

    private final Map<String, Integer> symbolTable;

    private int idx = 0;

    public MemoryBuilder() {
        memory = new ArrayList<>();
        symbolTable = new HashMap<>();
    }

    MemoryBuilder(final ArrayList<Object> memory, final Map<String, Integer> symbolTable) {
        this.memory = memory;
        this.symbolTable = symbolTable;
        this.idx = memory.size();
    }

    public void addSymbol(final String symbol) {
        if (!symbolTable.containsKey(symbol)) {
            memory.add(null);
            symbolTable.put(symbol, idx++);
        }
    }

    public void addSymbol(final String symbol, final Object value) {
        if (!symbolTable.containsKey(symbol)) {
            memory.add(value);
            symbolTable.put(symbol, idx++);
        } else {
            final int position = symbolTable.get(symbol).intValue();
            memory.ensureCapacity(position + 1);
            memory.set(position, value);
        }
    }


    public Pair<Object[], Map<String, Integer>> build() {
        return Pair.of(memory.toArray(), (Map<String, Integer>) new HashMap<>(symbolTable));
    }


    public MemoryBuilder copy() {
        return new MemoryBuilder(new ArrayList<>(memory), new HashMap<String, Integer>(symbolTable));
    }

}
