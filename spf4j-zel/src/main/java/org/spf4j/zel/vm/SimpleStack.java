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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.spf4j.base.Arrays;

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
     * construct a stack with specified size
     */
    public SimpleStack(final int size) {
        elems = (T[]) new Object[size];
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
        int t = top + 1;
        ensureCapacity(t);
        elems[top] = o;
        top = t;
    }

    private void ensureCapacity(final int minCapacity) {
        int oldCapacity = elems.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            // minCapacity is usually close to size, so this is a win:
            elems = java.util.Arrays.copyOf(elems, newCapacity);
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
        final T o = elems[--top];
        // this is for the garbage collector  to avoid memory leaks if storing big objects in stack
        elems[top] = null;
        return o;
    }

    public void remove() {
        --top;
        // this is for the garbage collector  to avoid memory leaks if storing big objects in stack
        elems[top] = null;
    }


    public T[] pop(final int n) {
        int ot = top;
        top -= n;
        T[] result =  (T[]) new Object[n];
        for (int i = top, j = 0; i < ot; i++, j++) {
            result[j] = elems[i];
            elems[i] = null;
        }
        return result;
    }
    
    public void popTo(final T[] to, final int n) {
        int ot = top;
        top -= n;
        for (int i = top, j = 0; i < ot; i++, j++) {
            to[j] = elems[i];
            elems[i] = null;
        }
    }

    public void removeFromTop(final int n) {
        int ot = top;
        top -= n;
        for (int i = top; i < ot; i++) {
            elems[i] = null;
        }
    }


    public T[] popUntil(final T until) {
        int i = top - 1;
        while (elems[i] != until) {
            i--;
        }
        T[] result = java.util.Arrays.copyOfRange(elems, i + 1, top);
        java.util.Arrays.fill(elems, i, top, null);
        top = i;
        return result;
    }


    /**
     * take a look at the top of stack
     *
     * @return Object
     */
    public T peek() {
        return elems[top - 1];
    }

    public T peekFromTop(final int n) {
        return elems[top - 1 - n];
    }

    public void replaceFromTop(final int n, final T value) {
        elems[top - 1 - n] = value;
    }



    public T[] peek(final int n) {
        return java.util.Arrays.copyOfRange(elems, top - n, top);
    }

    public T[] peekUntil(final T until) {
        int i = top - 1;
        while (elems[i] != until) {
            i--;
        }
        return java.util.Arrays.copyOfRange(elems, i + 1, top);
    }

    public T peekElemAfter(final T until) {
        int i = top - 1;
        while (elems[i] != until) {
            i--;
        }
        return elems[i - 1];
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
                    "Trying to get from invalid index: " + ptr + " from: " + this);
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
        result.append(elems[0]);
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
    public boolean contains(final Object o) {
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
    public <T> T[] toArray(final T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean add(final T e) {
        push(e);
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
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
        return Arrays.deepEquals(this.elems, other.elems, 0, top);
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
        for (int i = 0; i < top; i++) {
            Object obj = elems[i];
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T get(final int index) {
        return elems[index];
    }

    @Override
    public T set(final int index, final T element) {
        ensureCapacity(index);
        T result = elems[index];
        elems[index] = element;
        return result;
    }

    @Override
    public void add(final int index, final T element) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T remove(final int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int indexOf(final Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int lastIndexOf(final Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
