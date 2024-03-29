/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import org.spf4j.zel.instr.Instruction;


@SuppressFBWarnings("CD_CIRCULAR_DEPENDENCY")
public final class CompileContext implements ParsingContext {

    private final ProgramBuilder prog;

    private final MemoryBuilder staticMemBuilder;

    private Instruction last;

    CompileContext(final MemoryBuilder staticMemBuilder) {
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
    public void staticSymbol(final String name) {
        staticMemBuilder.addSymbol(name);
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
