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
package org.spf4j.io.tcp;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;


/**
 *
 * @author zoly
 */
public abstract class SelectorEventHandler {

    /**
     * Method must be invoked in selector thread.
     * THis will register this handler to Selector + channel.
     * @return
     * @throws ClosedChannelException
     */
    public abstract SelectionKey initialInterestRegistration() throws ClosedChannelException;

    public abstract boolean canRunAsync();

    public abstract void runAsync(SelectionKey key)
            throws IOException;

    public abstract void run(SelectionKey key) throws IOException;

}
