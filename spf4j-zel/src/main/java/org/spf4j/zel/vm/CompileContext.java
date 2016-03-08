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

import java.util.Arrays;
import org.spf4j.zel.instr.Instruction;


@edu.umd.cs.findbugs.annotations.SuppressWarnings("CD_CIRCULAR_DEPENDENCY")
public final class CompileContext implements ParsingContext {

    private final ProgramBuilder prog;

    private final MemoryBuilder staticMemBuilder;

    private Instruction last;

    public CompileContext(final MemoryBuilder staticMemBuilder) {
        this.prog = new ProgramBuilder(staticMemBuilder);
        this.staticMemBuilder = staticMemBuilder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void generateCode(final Location[] locs, final Instruction... args) {
        prog.addAll(args, Arrays.asList(locs));
        last = args[args.length - 1];
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
        return new CompileContext(staticMemBuilder);
    }

    @Override
    public void staticSymbol(final String name, final Object object) {
        staticMemBuilder.addSymbol(name, object);
    }

    @Override
    public Instruction getLast() {
       return last;
    }

    @Override
    public void generateCode(final Location loc, final Instruction instr) {
        prog.add(instr, loc);
        last = instr;
    }

    @Override
    public String toString() {
        return "CompileContext{" + "prog=" + prog + ", staticMemBuilder=" + staticMemBuilder + ", last=" + last + '}';
    }

}
