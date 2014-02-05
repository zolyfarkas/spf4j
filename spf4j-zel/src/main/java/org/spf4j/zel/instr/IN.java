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

import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.ZExecutionException;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * @author zoly
 * @version 1.0
 */
public final class IN extends Instruction {

    private static final long serialVersionUID = 8486583432881741964L;

    private IN() {
    }
    
    @Override
    public void execute(final ExecutionContext context) throws ZExecutionException {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(context.in);
            context.push(ois.readObject());
            context.ip++;
        } catch (IOException ex) {
            throw new ZExecutionException("IO error when reading from input Stream", ex, context);
        } catch (ClassNotFoundException ex) {
            throw new ZExecutionException("Unsupported object in stream", ex, context);
        }
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new IN();

}
