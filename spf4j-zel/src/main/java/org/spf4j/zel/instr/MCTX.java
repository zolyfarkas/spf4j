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
package org.spf4j.zel.instr;

import java.math.MathContext;
import org.spf4j.zel.operators.Operator;
import org.spf4j.zel.vm.ExecutionContext;


public final class MCTX extends Instruction {

    private static final long serialVersionUID = 6127414006563169983L;

    private final int precission;
    
    public MCTX(final int precission) {
        this.precission = precission;
    }

    @Override
    public int execute(final ExecutionContext context) {
        MathContext mc;
        switch (precission) {
            case 32:
                mc = MathContext.DECIMAL32;
                break;
            case 64:
                mc = MathContext.DECIMAL64;
                break;
            case 128:
                mc = MathContext.DECIMAL128;
                break;
            default:
                mc = new MathContext(precission);
        }
        context.setMathContext(mc);
        Operator.MATH_CONTEXT.set(mc);
        return 1;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {precission};
    }

}
