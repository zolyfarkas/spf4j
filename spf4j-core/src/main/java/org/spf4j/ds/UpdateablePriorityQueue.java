package org.spf4j.ds;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * A priority queue which allows efficient element update.
 * Element update can be either a replacement or element mutation.
 * Implementation is based on jdk priority queue.
 * @author zoly
 * @param <E>
 */

public final class UpdateablePriorityQueue<E> implements Iterable<E>, Serializable {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final long serialVersionUID = 1L;

    public final class ElementRef implements Comparable<ElementRef> {

        private static final long serialVersionUID = 1L;

        private E elem;
        private int index;

        ElementRef(@Nonnull final E elem, final int idx) {
            this.elem = elem;
            this.index = idx;
        }

        @Override
        public int compareTo(final ElementRef e) {
            return comparator.compare(elem, e.elem);
        }

        public E getElem() {
            return elem;
        }

        public int getIndex() {
            return index;
        }

        public void setElem(final E elem) {

            final int compare = comparator.compare(this.elem, elem);
            this.elem = elem;
            if (compare > 0) {
                siftUp(index, this);
            } else if (compare < 0) {
                siftDown(index, this);
            }
        }

        public void elementMutated() {
            int idx = this.index;
            siftUp(this.index, this);
            if (idx == this.index) {
                siftDown(idx, this);
            }
        }

        public boolean remove() {
            if (this.index < 0) {
                return false;
            }
            removeAt(this.index);
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.elem);
            return 97 * hash + this.index;
        }


        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ElementRef other = (ElementRef) obj;
            if (!Objects.equals(this.elem, other.elem)) {
                return false;
            }
            return this.index == other.index;
        }



        @Override
        public String toString() {
            return "QueueElement{" + "elem=" + elem + ", index=" + index + '}';
        }

    }

    private transient ElementRef[] queue;

    private int size = 0;


    private final Comparator<? super E> comparator;

    /**
     * modification count;
     */
    private transient int modCount = 0;

    public UpdateablePriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    public UpdateablePriorityQueue(final int initialCapacity) {
        this(initialCapacity, null);
    }


    private static final Comparator<? extends Comparable> DEFAULT_COMPARATOR =
            new Comparator<Comparable>() {

                @Override
                public int compare(final Comparable e1, final Comparable e2) {
                    return e1.compareTo(e2);
                }
            };

    public UpdateablePriorityQueue(final int initialCapacity,
            final Comparator<? super E> comparator) {

        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Invalid initial capacity " + initialCapacity);
        }
        this.queue = (ElementRef[]) Array.newInstance(ElementRef.class, initialCapacity);
        if (comparator == null) {
            this.comparator = (Comparator<? super E>) DEFAULT_COMPARATOR;
        } else {
            this.comparator = comparator;
        }
    }

    private void grow(final int minCapacity) {
        if (minCapacity < 0) { // overflow
            throw new OutOfMemoryError();
        }
        int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = ((oldCapacity < 64) ? ((oldCapacity + 1) * 2) : ((oldCapacity / 2) * 3));
        if (newCapacity < 0) { // overflow
            newCapacity = Integer.MAX_VALUE;
        }
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        queue = Arrays.copyOf(queue, newCapacity);
    }

    public ElementRef add(@Nonnull final E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        modCount++;
        int i = size;
        if (i >= queue.length) {
            grow(i + 1);
        }
        size = i + 1;
        ElementRef qe = new ElementRef(e, 0);
        if (i == 0) {
            queue[0] = qe;
        } else {
            siftUp(i, qe);
        }
        return qe;
    }

    @Nullable
    public E peek() {
        if (size == 0) {
            return null;
        }
        return queue[0].elem;
    }

    @Nullable
    public ElementRef peekEntry() {
        if (size == 0) {
            return null;
        }
        return queue[0];
    }


    private int indexOf(final E o) {
        if (o != null) {
            for (int i = 0; i < size; i++) {
                if (o.equals(queue[i].elem)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean remove(final ElementRef qe) {
        int i = qe.index;
        if (i == -1) {
            return false;
        } else {
            removeAt(i);
            return true;
        }
    }

    public boolean remove(final E o) {
        int i = indexOf(o);
        if (i == -1) {
            return false;
        } else {
            removeAt(i);
            return true;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    boolean removeEq(final Object o) {
        for (int i = 0; i < size; i++) {
            if (o == queue[i]) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(final E o) {
        return indexOf(o) != -1;
    }

    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }


    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] a) {
        if (a.length < size) { // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(queue, size, a.getClass());
        }
        System.arraycopy(queue, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private final class Itr implements Iterator<E> {

        /**
         * Index (into queue array) of element to be returned by subsequent call to next.
         */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next, unless that element came from the forgetMeNot list.
         * Set to -1 if element is deleted by a call to remove.
         */
        private int lastRet = -1;

        /**
         * A queue of elements that were moved from the unvisited portion of the heap into the visited portion as a
         * result of "unlucky" element removals during the iteration. (Unlucky element removals are those that require a
         * siftup instead of a siftdown.) We must visit all of the elements in this list to complete the iteration. We
         * do this after we've completed the "normal" iteration.
         *
         * We expect that most iterations, even those involving removals, will not need to store elements in this field.
         */
        private ArrayDeque<E> forgetMeNot = null;

        /**
         * Element returned by the most recent call to next iff that element was drawn from the forgetMeNot list.
         */
        private E lastRetElt = null;


        private int expectedModCount = modCount;

        @Override
        public boolean hasNext() {
            return cursor < size
                    || (forgetMeNot != null && !forgetMeNot.isEmpty());
        }

        @Override
        public E next() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (cursor < size) {
                //CHECKSTYLE:OFF
                return queue[lastRet = cursor++].elem;
                //CHECKSTYLE:ON
            }
            if (forgetMeNot != null) {
                lastRet = -1;
                lastRetElt = forgetMeNot.poll();
                if (lastRetElt != null) {
                    return lastRetElt;
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (lastRet != -1) {
                E moved = UpdateablePriorityQueue.this.removeAt(lastRet);
                lastRet = -1;
                if (moved == null) {
                    cursor--;
                } else {
                    if (forgetMeNot == null) {
                        forgetMeNot = new ArrayDeque<>();
                    }
                    forgetMeNot.add(moved);
                }
            } else if (lastRetElt != null) {
                UpdateablePriorityQueue.this.removeEq(lastRetElt);
                lastRetElt = null;
            } else {
                throw new IllegalStateException();
            }
            expectedModCount = modCount;
        }
    }

    public int size() {
        return size;
    }

    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++) {
            final ElementRef ref = queue[i];
            ref.index = -1;
            queue[i] = null;
        }
        size = 0;
    }

    @Nullable
    public E poll() {
        if (size == 0) {
            return null;
        }
        int s = --size;
        modCount++;
        final ElementRef removedRef = queue[0];
        removedRef.index = -1;
        E result = removedRef.elem;
        ElementRef x = queue[s];
        queue[s] = null;
        if (s != 0) {
            siftDown(0, x);
        }
        return result;
    }


    private E removeAt(final int i) {
        assert i >= 0 && i < size;
        modCount++;
        queue[i].index = -1;
        int s = --size;
        if (s == i) { // removed last element
            queue[i] = null;
        } else {
            ElementRef moved = queue[s];
            queue[s] = null;
            siftDown(i, moved);
            if (queue[i] == moved) {
                siftUp(i, moved);
                if (queue[i] != moved) {
                    return moved.elem;
                }
            }
        }
        return null;
    }


    private void siftUp(final int pk, final ElementRef x) {
        int k = pk;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            ElementRef e = queue[parent];
            if (x.compareTo(e) >= 0) {
                break;
            }
            queue[k] = e;
            e.index = k;
            k = parent;
        }
        queue[k] = x;
        x.index = k;
    }

    private void siftDown(final int pk, final ElementRef x) {
        int k = pk;
        int half = size >>> 1;        // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            ElementRef c = queue[child];
            int right = child + 1;
            if (right < size && c.compareTo(queue[right]) > 0) {
                //CHECKSTYLE:OFF
                c = queue[child = right];
                //CHECKSTYLE:ON
            }
            if (x.compareTo(c) <= 0) {
                break;
            }
            queue[k] = c;
            c.index = k;
            k = child;
        }
        queue[k] = x;
        x.index = k;
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }


    private void writeObject(final java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        // Write out array length, for compatibility with 1.5 version
        s.writeInt(Math.max(2, size + 1));

        // Write out all elements in the "proper order".
        for (int i = 0; i < size; i++) {
            s.writeObject(queue[i].elem);
        }
    }


    private void readObject(final java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in (and discard) array length
        s.readInt();

        queue = (ElementRef[]) Array.newInstance(ElementRef.class, size);

        // Read in all elements.
        for (int i = 0; i < size; i++) {
            queue[i] = new ElementRef((E) s.readObject(), i);
        }

        // Elements are guaranteed to be in "proper order", but the
        // spec has never explained what that might be.
        heapify();
    }

    public void heapify() {
        for (int i = (size >>> 1) - 1; i >= 0; i--) {
            siftDown(i, queue[i]);
        }
    }

}
