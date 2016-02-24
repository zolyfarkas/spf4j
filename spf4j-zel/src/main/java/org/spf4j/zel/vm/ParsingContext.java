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

import java.io.Serializable;
import org.spf4j.zel.instr.Instruction;


public interface ParsingContext {


    final class Location implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int row;
        private final int column;

        public Location(final int row, final int column) {
            this.row = row - 1;
            this.column = column;
        }

        @Override
        public String toString() {
            return "Location{" + "row=" + row + ", column=" + column + '}';
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

    }


    /**
     * generate instruction code with argument
     *
     * @param instr Instruction
     * @param arg Object
     */
    void generateCode(Location[] loc, Instruction... args);


    void generateCode(Location loc, Instruction instr);


    void staticSymbol(String name, Object object);

    /**
     * Add code to this context
     *
     * @param code Object[]
     */
    void generateCodeAll(ParsingContext parsingContext);


    /**
     * return the current code address
     *
     * @return
     */
    int getAddress();


    Instruction getLast();

    /**
     * send a object for processing
     *
     * @param obj Object
     */
    void process(Object obj);

    /**
     * get the code generated in this context
     *
     * @return Object[]
     */
    ProgramBuilder getProgramBuilder();

    /**
     * clone this context
     *
     * @return
     */
    ParsingContext createSubContext();

}
