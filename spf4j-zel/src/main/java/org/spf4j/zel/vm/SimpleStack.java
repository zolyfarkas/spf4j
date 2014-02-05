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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public final class SimpleStack<T>
        implements List<T> {

    /**
     * the stack storage
     */
    private T[] elems;
    /**
     * the top element position
     */
    private int top;
    /**
     * stack default initial size
     */
    private static final int DEFAULT_SIZE = 32;

    /**
     * construct a stack with specified max size
     *
     * @param max int
     */
    public SimpleStack(final int max) {

        elems = (T[]) new Object[max];
        top = 0;
    }

    /**
     * Construct a stack, default size is 20
     */
    public SimpleStack() {
        this(DEFAULT_SIZE);
    }

    /**
     * check if stack is empty
     *
     * @return boolean
     */
    @Override
    public boolean isEmpty() {
        return top == 0;
    }

    /**
     * push object into stack
     *
     * @param o Object
     */
    public void push(final T o) {
        ensureCapacity(top);
        elems[top++] = o;
    }

    private void ensureCapacity(final int minCapacity) {
        int oldCapacity = elems.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            // minCapacity is usually close to size, so this is a win:
            elems = Arrays.copyOf(elems, newCapacity);
        }
    }
    
    

    /**
     * Push more objects into the stack
     *
     * @param args
     */
    public void pushAll(final T[] args) {
        if (args.length == 0) {
            return;
        }
        int newTop = top + args.length;
        ensureCapacity(newTop);
        System.arraycopy(args, 0, elems, top, args.length);
        top = newTop;
    }

    /**
     * pops object out of stack
     *
     * @return Object
     */
    public T pop() {
        final T o = elems[top - 1];
        top--;
        // this is for the garbage collector  to avoid memory leaks if storing big objects in stack
        elems[top] = null;
        return o;
    }
    

    /**
     * take a look at the top of stack
     *
     * @return Object
     */
    public T peek() {
        return elems[top - 1];
    }

    /**
     * Clear the stack - also makes sure the stack objects are not referenced anymore
     */
    @Override
    public void clear() {
        for (int i = 0; i < top; i++) {
            elems[i] = null;
        }
        top = 0;
    }

    /**
     * get the curent stack pos relative to base
     *
     * @return
     */
    public int getPtr() {
        return top;
    }

    /**
     * get element from stack at index relative to base
     *
     * @param ptr
     * @return
     */
    public T getFromPtr(final int ptr) {
        if (ptr < 0 || ptr >= top) {
            throw new IndexOutOfBoundsException(
                    "Trying to get from invalid index: " + ptr + " from: " + this.toString());
        }
        return elems[ptr];
    }

    /**
     * returns a character separated string with the stack elements
     *
     * @param separator
     * @return String
     */
    public String toString(final char separator) {
        if (top == 0) {
            return "";
        }
        final StringBuilder result = new StringBuilder(32);
        result.append(elems[0].toString());
        for (int i = 1; i < top; i++) {
            result.append(separator);
            result.append(elems[i]);
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return toString('.');
    }

    @Override
    public int size() {
        return top;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean add(T e) {
        push(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleStack<T> other = (SimpleStack<T>) obj;
        if (this.top != other.top) {
            return false;
        }
        return (Arrays.deepEquals(this.elems, other.elems));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Arrays.deepHashCode(this.elems);
        hash = 37 * hash + this.top;
        return hash;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T get(int index) {
        return elems[index];
    }

    @Override
    public T set(int index, T element) {
        ensureCapacity(index);
        T result = elems[index];
        elems[index] = element;
        return result;
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
