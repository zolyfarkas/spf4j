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


public final class CompileContext implements ParsingContext {

    private final ProgramBuilder prog;

    public CompileContext() {
        this.prog = new ProgramBuilder();
    }

    
    @SuppressWarnings("unchecked")
    @Override
    public void generateCode(final Object... args) {
        if (args == null) {
            prog.add(null);
        } else {
            prog.addAll(args);
        }
    }

    @Override
    public void generateCodeAt(final int address, final Object... args) {
        if (args == null) {
            prog.set(address, null);
        } else {
            prog.setAll(address, args);
        }
    }

    @Override
    public void generateCodeAll(final ParsingContext parsingContext) {
        prog.addAll(parsingContext.getProgramBuilder());
    }

    @Override
    public int getAddress() {
        return prog.size();
    }

    /**
     * Do no aditional processing
     *
     * @param obj Object
     */
    @Override
    public void process(final Object obj) {
    }


    @Override
    public ProgramBuilder getProgramBuilder() {
        return prog;
    }

    @Override
    public CompileContext createSubContext() {
        return new CompileContext();
    }


}
