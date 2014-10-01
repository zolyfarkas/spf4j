
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
package org.spf4j.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.spf4j.io.ByteArrayBuilder;

/**
 *
 * @author zoly
 */
public final class Objects {

    private Objects() {
    }

    public static <T extends Serializable> T clone(final T t) {
        try (ByteArrayBuilder bos = new ByteArrayBuilder(256);
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(t);
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.getBuffer(), 0, bos.size()))) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
