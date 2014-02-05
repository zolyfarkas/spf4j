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

class TmpParsingContext implements ParsingContext {

    /**
     * Construct A TmpExprContext with the relay context
     *
     * @param ectx ExprContext
     */
    public TmpParsingContext(ParsingContext ectx) {
        this.ectx = ectx;
    }
    /**
     * The relay context
     */
    final ParsingContext ectx;
    /**
     * The generated code
     */
    final ProgramBuilder code = new ProgramBuilder();

    /**
     * generate instruction code with argument
     *
     * @param instr Instruction
     * @param arg Object
     */
    @Override
    public final void generateCode(Object... args) {
        if (args == null) {
            code.add(null);
        } else {
            code.addAll(args);
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

    /**
     * Relay processing to the relay context
     *
     * @param obj Object
     */
    @Override
    public void process(Object obj) {
        ectx.process(obj);
    }

    /**
     * return code generated in this context
     *
     * @return Object[]
     */
    @Override
    public ProgramBuilder getProgramBuilder() {
        return code;
    }

    /**
     * Emty implement don't need it now
     *
     * @param code Object[]
     */
    @Override
    public void generateCodeAll(ParsingContext parsingContext) {
        this.code.addAll(parsingContext.getProgramBuilder());
    }

    @Override
    public TmpParsingContext createSubContext() {
        return this;
    }

    @Override
    public void generateCodeAt(int address, Object... args) {
        if (args == null) {
            code.set(address, null);
        } else {
            code.setAll(address, args);
        }
    }
}
