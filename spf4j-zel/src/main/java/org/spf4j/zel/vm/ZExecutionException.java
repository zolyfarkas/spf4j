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
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class ZExecutionException extends ExecutionException {

    private static final long serialVersionUID = 8823469923479284L;

    public ZExecutionException(final String message, final Exception e) {
        super(message, e);
        this.payload = null;
    }


    public ZExecutionException(final Exception e) {
        super(e);
        this.payload = null;
    }

    public ZExecutionException(final String msg) {
        super(msg);
        this.payload = null;
    }

    public ZExecutionException(final Object object) {
        super();
        this.payload = object;
    }


    private final Object payload;


    public final void addZelFrame(final ZelFrame frame) {
        zelframes.add(frame);
    }

    public List<ZelFrame> getZelframes() {
        return zelframes;
    }


    private final List<ZelFrame> zelframes = new ArrayList<>();

    @Override
    public String toString() {
        String msg = super.toString();
        StringBuilder result = new StringBuilder(1024);
        result.append(msg);
        result.append('\n');
        result.append("Zel trace:\n");
        for (ZelFrame frame : zelframes) {
            result.append(frame.toString());
            result.append('\n');
        }
        return result.toString();
    }


}
