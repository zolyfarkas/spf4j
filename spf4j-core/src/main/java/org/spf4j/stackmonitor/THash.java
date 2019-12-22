//CHECKSTYLE:OFF
///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001, Eric D. Friedman All Rights Reserved.
// Copyright (c) 2009, Rob Eden All Rights Reserved.
// Copyright (c) 2009, Jeff Randall All Rights Reserved.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////

package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.impl.Constants;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.PrimeFinder;

import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;



/**
 * Base class for hashtables that use open addressing to resolve
 * collisions.
 * removed compaction element to reduce memory footprint.
 * use case for this implementation does not have removes.
 *
 * Created: Wed Nov 28 21:11:16 2001
 *
 * @author Eric D. Friedman
 * @author Rob Eden (auto-compaction)
 * @author Jeff Randall
 * @author zfarkas
 *
 * @version $Id: THash.java,v 1.1.2.4 2010/03/02 00:55:34 robeden Exp $
 */
@SuppressFBWarnings
abstract public class THash implements Externalizable {
    @SuppressWarnings( { "UnusedDeclaration" } )
    static final long serialVersionUID = -1792948471915530295L;

    /** the load above which rehashing occurs. */
    protected static final float DEFAULT_LOAD_FACTOR = Float.parseFloat(System.getProperty("spf4j.methodMap.loadFactor",
            "0.7"));

    /**
     * the default initial capacity for the hash table.  This is one
     * less than a prime value because one is added to it when
     * searching for a prime capacity to account for the free slot
     * required by open addressing. Thus, the real default capacity is
     * 11.
     */
    protected static final int DEFAULT_CAPACITY = Constants.DEFAULT_CAPACITY;


    /** the current number of occupied slots in the hash. */
    protected transient int _size;

    /**
     * The maximum number of elements allowed without allocating more
     * space.
     */
    protected int _maxSize;


    /**
     * Creates a new <code>THash</code> instance with the default
     * capacity and load factor.
     */
    public THash() {
        this( DEFAULT_CAPACITY );
    }




    /**
     * Creates a new <code>THash</code> instance with a prime capacity
     * at or near the minimum needed to hold <tt>initialCapacity</tt>
     * elements with load factor <tt>loadFactor</tt> without triggering
     * a rehash.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor      a <code>float</code> value
     */
    public THash( int initialCapacity) {
        super();
        setUp( HashFunctions.fastCeil( initialCapacity / DEFAULT_LOAD_FACTOR ) );
    }


    /**
     * Tells whether this set is currently holding any elements.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEmpty() {
        return 0 == _size;
    }


    /**
     * Returns the number of distinct elements in this collection.
     *
     * @return an <code>int</code> value
     */
    public int size() {
        return _size;
    }


    /** @return the current physical capacity of the hash table. */
    abstract public int capacity();


    /**
     * Ensure that this hashtable has sufficient capacity to hold
     * <tt>desiredCapacity<tt> <b>additional</b> elements without
     * requiring a rehash.  This is a tuning method you can call
     * before doing a large insert.
     *
     * @param desiredCapacity an <code>int</code> value
     */

    public void ensureCapacity(int desiredCapacity) {
        int requiredSize = desiredCapacity + _size;
        if (requiredSize > _maxSize) {
            rehash(PrimeFinder.nextPrime(Math.max( _size + (_size >> 1) + 1,
				HashFunctions.fastCeil((requiredSize) / DEFAULT_LOAD_FACTOR ) + 1) ) );
            computeMaxSize(capacity());
        }
    }


    /**
     * Delete the record at <tt>index</tt>.  Reduces the size of the
     * collection by one.
     *
     * @param index an <code>int</code> value
     */
    protected void removeAt( int index ) {
        _size--;
    }


    /** Empties the collection. */
    public void clear() {
        _size = 0;
    }


    /**
     * initializes the hashtable to a prime capacity which is at least
     * <tt>initialCapacity + 1</tt>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    protected int setUp( int initialCapacity ) {
        if (initialCapacity == 0) {
          return 0;
        }
        int capacity;

        capacity = PrimeFinder.nextPrime( initialCapacity );
        computeMaxSize( capacity );

        return capacity;
    }


    /**
     * Rehashes the set.
     *
     * @param newCapacity an <code>int</code> value
     */
    protected abstract void rehash( int newCapacity );



    /**
     * Computes the values of maxSize. There will always be at least
     * one free slot required.
     *
     * @param capacity an <code>int</code> value
     */
    protected void computeMaxSize( int capacity ) {
        // need at least one free slot for open addressing
        _maxSize = Math.min( capacity - 1, (int) ( capacity * DEFAULT_LOAD_FACTOR ) );
    }


    /**
     * After an insert, this hook is called to adjust the size/free
     * values of the set and to perform rehashing if necessary.
     *
     * @param usedFreeSlot the slot
     */
    protected final void postInsertHook( boolean usedFreeSlot ) {
        ++_size;
    }


    protected int calculateGrownCapacity() {
        return capacity() << 1;
    }


    public void writeExternal( ObjectOutput out ) throws IOException {
        // VERSION
        out.writeByte( 0 );
    }


    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException {

        // VERSION
        in.readByte();


        setUp( (int) Math.ceil( DEFAULT_CAPACITY / DEFAULT_LOAD_FACTOR ) );
    }
}// THash