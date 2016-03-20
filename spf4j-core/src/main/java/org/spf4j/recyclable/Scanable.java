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
package org.spf4j.recyclable;

/**
 *
 * @author zoly
 * @param <T> - type of the objects to scan.
 */
public interface Scanable<T> {
    
    /**
     * Scan through  objects and call handler...
     * if handler throws exception scan is not aborted.
     * scan is aborted only if handler returns false.
     * @param handler - the handler to handle the scanned objects.
     * @return - false if scanning is stopped by the handler, true otherwise.
     * @throws Exception - whatever exception is thrown during scanning.
     */
    boolean scan(ScanHandler<T> handler) throws Exception;
            
    interface ScanHandler<O> {
        
        /**
         * method to handle object
         * @param object - the scanned object.
         * @return true if scan operation is to continue, false otherwise.
         * @throws java.lang.Exception - whatever exception this handler needs to throw.
         */
        boolean handle(O object) throws Exception;
    }
    
}
