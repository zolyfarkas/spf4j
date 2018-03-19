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
package org.spf4j.io.tcp.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public interface Sniffer {

    /**
     * Invoked on data receive/transmission.
     * @param data - the byte buffer containing the data.
     * @param nrBytes - number of bytes in the buffer. The data in the buffer is from position-nrBytes to position.
     * nrBytes will be -1 on EOF.
     * @return new nrReadValue if we aim to mutate buffer, returning -1 will simulate a EOF.
     */
    int received(ByteBuffer data, int nrBytes);


    /**
     * Allows to intercept read errors and change/suppress them.
     * @param ex
     * @return A exception you want to propagate, or null in case we do not want to propagate exception.
     */
    @Nullable
    default IOException received(final IOException ex) {
      return ex;
    }

}
