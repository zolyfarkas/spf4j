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

import org.spf4j.zel.instr.Instruction;

/**
 *
 * @author zoly
 */
public final class ConstantParsingContext implements ParsingContext {

    private Object obj;

    @Override
    public void generateCode(final Object... args) {
        for (Object o : args) {
            if (!(o instanceof Instruction)) {
                obj = o;
            }
        }

    }

    /**
     * return the current code address
     *
     * @return
     */
    @Override
    public int getAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void process(final Object p) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ProgramBuilder getProgramBuilder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generateCodeAll(final ParsingContext parsingContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ConstantParsingContext createSubContext() {
        return this;
    }

    public Object getConstant() {
        return obj;
    }

    @Override
    public void generateCodeAt(final int address, final Object... args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
