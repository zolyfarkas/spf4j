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
package org.spf4j.text;
//CHECKSTYLE:OFF
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.Annotation;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import static java.text.CharacterIterator.DONE;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An AttributedString holds text and related attribute information. It
 * may be used as the actual data storage in some cases where a text
 * reader wants to access attributed text through the AttributedCharacterIterator
 * interface.
 *
 * <p>
 * An attribute is a key/value pair, identified by the key.  No two
 * attributes on a given character can have the same key.
 *
 * <p>The values for an attribute are immutable, or must not be mutated
 * by clients or storage.  They are always passed by reference, and not
 * cloned.
 *
 * @see AttributedCharacterIterator
 * @see Annotation
 * @since 1.2
 */

@SuppressFBWarnings({ "PL_PARALLEL_LISTS" }) // uses Vector specific setSize().
public final class AttributedString {

    // since there are no vectors of int, we have to use arrays.
    // We allocate them in chunks of 10 elements so we don't have to allocate all the time.
    private static final int ARRAY_SIZE_INCREMENT = 10;

    // field holding the text
    private String text;

    // fields holding run attribute information
    // run attributes are organized by run
    private int runArraySize;               // current size of the arrays
    private int runCount;                   // actual number of runs, <= runArraySize
    private int runStarts[];                // start index for each run
    private ArrayList<Attribute> runAttributes[];         // vector of attribute keys for each run
    private ArrayList<Object> runAttributeValues[];    // parallel vector of attribute values for each run

    /**
     * Constructs an AttributedString instance with the given
     * AttributedCharacterIterators.
     *
     * @param iterators AttributedCharacterIterators to construct
     * AttributedString from.
     * @throws NullPointerException if iterators is null
     */
    AttributedString(@Nonnull AttributedCharacterIterator[] iterators) {
        if (iterators.length == 0) {
            text = "";
        }
        else {
            // Build the String contents
            StringBuilder buffer = new StringBuilder();
            for (int counter = 0; counter < iterators.length; counter++) {
                appendContents(buffer, iterators[counter]);
            }

            text = buffer.toString();

            if (text.length() > 0) {
                // Determine the runs, creating a new run when the attributes
                // differ.
                int offset = 0;
                Map<Attribute,Object> last = null;

                for (int counter = 0; counter < iterators.length; counter++) {
                    AttributedCharacterIterator iterator = iterators[counter];
                    int start = iterator.getBeginIndex();
                    int end = iterator.getEndIndex();
                    int index = start;

                    while (index < end) {
                        iterator.setIndex(index);

                        Map<Attribute,Object> attrs = iterator.getAttributes();

                        if (mapsDiffer(last, attrs)) {
                            setAttributes(attrs, index - start + offset);
                        }
                        last = attrs;
                        index = iterator.getRunLimit();
                    }
                    offset += (end - start);
                }
            }
        }
    }

    /**
     * Constructs an AttributedString instance with the given text.
     * @param text The text for this attributed string.
     * @exception NullPointerException if <code>text</code> is null.
     */
    public AttributedString(@Nonnull String text) {
        this.text = text;
    }

    /**
     * Constructs an AttributedString instance with the given text and attributes.
     * @param text The text for this attributed string.
     * @param attributes The attributes that apply to the entire string.
     * @exception NullPointerException if <code>text</code> or
     *            <code>attributes</code> is null.
     * @exception IllegalArgumentException if the text has length 0
     * and the attributes parameter is not an empty Map (attributes
     * cannot be applied to a 0-length range).
     */
    public AttributedString(@Nonnull String text,
                            @Nonnull Map<? extends Attribute, ?> attributes)
    {
        this.text = text;

        if (text.length() == 0) {
            if (attributes.isEmpty())
                return;
            throw new IllegalArgumentException("Can't add attribute to 0-length text: " + attributes);
        }

        int attributeCount = attributes.size();
        if (attributeCount > 0) {
            createRunAttributeDataVectors();
            ArrayList<Attribute> newRunAttributes = new ArrayList<>(attributeCount);
            ArrayList<Object> newRunAttributeValues = new ArrayList<>(attributeCount);
            runAttributes[0] = newRunAttributes;
            runAttributeValues[0] = newRunAttributeValues;

            Iterator<? extends Map.Entry<? extends Attribute, ?>> iterator = attributes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<? extends Attribute, ?> entry = iterator.next();
                newRunAttributes.add(entry.getKey());
                newRunAttributeValues.add(entry.getValue());
            }
        }
    }

    /**
     * Constructs an AttributedString instance with the given attributed
     * text represented by AttributedCharacterIterator.
     * @param text The text for this attributed string.
     * @exception NullPointerException if <code>text</code> is null.
     */
    public AttributedString(AttributedCharacterIterator text) {
        // If performance is critical, this constructor should be
        // implemented here rather than invoking the constructor for a
        // subrange. We can avoid some range checking in the loops.
        this(text, text.getBeginIndex(), text.getEndIndex(), null);
    }

    /**
     * Constructs an AttributedString instance with the subrange of
     * the given attributed text represented by
     * AttributedCharacterIterator. If the given range produces an
     * empty text, all attributes will be discarded.  Note that any
     * attributes wrapped by an Annotation object are discarded for a
     * subrange of the original attribute range.
     *
     * @param text The text for this attributed string.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last character
     * of the range.
     * @exception NullPointerException if <code>text</code> is null.
     * @exception IllegalArgumentException if the subrange given by
     * beginIndex and endIndex is out of the text range.
     * @see java.text.Annotation
     */
    public AttributedString(AttributedCharacterIterator text,
                            int beginIndex,
                            int endIndex) {
        this(text, beginIndex, endIndex, null);
    }

    /**
     * Constructs an AttributedString instance with the subrange of
     * the given attributed text represented by
     * AttributedCharacterIterator.  Only attributes that match the
     * given attributes will be incorporated into the instance. If the
     * given range produces an empty text, all attributes will be
     * discarded. Note that any attributes wrapped by an Annotation
     * object are discarded for a subrange of the original attribute
     * range.
     *
     * @param text The text for this attributed string.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last character
     * of the range.
     * @param attributes Specifies attributes to be extracted
     * from the text. If null is specified, all available attributes will
     * be used.
     * @exception NullPointerException if <code>text</code> is null.
     * @exception IllegalArgumentException if the subrange given by
     * beginIndex and endIndex is out of the text range.
     * @see java.text.Annotation
     */
    @SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD")
    public AttributedString(@Nonnull AttributedCharacterIterator text,
                            int beginIndex,
                            int endIndex,
                            Attribute[] attributes) {
        // Validate the given subrange
        int textBeginIndex = text.getBeginIndex();
        int textEndIndex = text.getEndIndex();
        if (beginIndex < textBeginIndex || endIndex > textEndIndex || beginIndex > endIndex)
            throw new IllegalArgumentException("Invalid substring range " + beginIndex + ' ' + endIndex);

        // Copy the given string
        StringBuilder textBuffer = new StringBuilder(endIndex - beginIndex);
        text.setIndex(beginIndex);
        for (char c = text.current(); text.getIndex() < endIndex; c = text.next())
            textBuffer.append(c);
        this.text = textBuffer.toString();

        if (beginIndex == endIndex)
            return;

        // Select attribute keys to be taken care of
        HashSet<Attribute> keys;
        Set<Attribute> allAttributeKeys = text.getAllAttributeKeys();
        if (attributes == null) {
            keys = new HashSet<>(allAttributeKeys);
        } else {
            keys = Sets.newHashSetWithExpectedSize(allAttributeKeys.size());
            for (int i = 0, l = attributes.length; i < l; i++) {
                Attribute attribute = attributes[i];
                if (allAttributeKeys.contains(attribute)) {
                  keys.add(attribute);
                }
            }
        }
        if (keys.isEmpty())
            return;

        // Get and set attribute runs for each attribute name. Need to
        // scan from the top of the text so that we can discard any
        // Annotation that is no longer applied to a subset text segment.
        Iterator<Attribute> itr = keys.iterator();
        while (itr.hasNext()) {
            Attribute attributeKey = itr.next();
            text.setIndex(textBeginIndex);
            while (text.getIndex() < endIndex) {
                int start = text.getRunStart(attributeKey);
                int limit = text.getRunLimit(attributeKey);
                Object value = text.getAttribute(attributeKey);

                if (value != null) {
                    if (value instanceof Annotation) {
                        if (start >= beginIndex && limit <= endIndex) {
                            addAttribute(attributeKey, value, start - beginIndex, limit - beginIndex);
                        } else {
                            if (limit > endIndex)
                                break;
                        }
                    } else {
                        // if the run is beyond the given (subset) range, we
                        // don't need to process further.
                        if (start >= endIndex)
                            break;
                        if (limit > beginIndex) {
                            // attribute is applied to any subrange
                            if (start < beginIndex)
                                start = beginIndex;
                            if (limit > endIndex)
                                limit = endIndex;
                            if (start != limit) {
                                addAttribute(attributeKey, value, start - beginIndex, limit - beginIndex);
                            }
                        }
                    }
                }
                text.setIndex(limit);
            }
        }
    }

    /**
     * Adds an attribute to the entire string.
     * @param attribute the attribute key
     * @param value the value of the attribute; may be null
     * @exception NullPointerException if <code>attribute</code> is null.
     * @exception IllegalArgumentException if the AttributedString has length 0
     * (attributes cannot be applied to a 0-length range).
     */
    public void addAttribute(@Nonnull Attribute attribute, Object value) {

        int len = length();
        if (len == 0) {
            throw new IllegalArgumentException("Can't add attribute to 0-length text: " + attribute);
        }

        addAttributeImpl(attribute, value, 0, len);
    }

    /**
     * Adds an attribute to a subrange of the string.
     * @param attribute the attribute key
     * @param value The value of the attribute. May be null.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last character of the range.
     * @exception NullPointerException if <code>attribute</code> is null.
     * @exception IllegalArgumentException if beginIndex is less then 0, endIndex is
     * greater than the length of the string, or beginIndex and endIndex together don't
     * define a non-empty subrange of the string.
     */
    public void addAttribute(@Nonnull Attribute attribute, Object value,
            int beginIndex, int endIndex) {

        if (beginIndex < 0 || endIndex > length() || beginIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid substring range " + beginIndex + ',' + endIndex);
        }

        addAttributeImpl(attribute, value, beginIndex, endIndex);
    }

    /**
     * Adds a set of attributes to a subrange of the string.
     * @param attributes The attributes to be added to the string.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last
     * character of the range.
     * @exception NullPointerException if <code>attributes</code> is null.
     * @exception IllegalArgumentException if beginIndex is less then
     * 0, endIndex is greater than the length of the string, or
     * beginIndex and endIndex together don't define a non-empty
     * subrange of the string and the attributes parameter is not an
     * empty Map.
     */
    public void addAttributes(@Nonnull Map<? extends Attribute, ?> attributes,
                              int beginIndex, int endIndex)
    {
        if (beginIndex < 0 || endIndex > length() || beginIndex > endIndex) {
            throw new IllegalArgumentException("Invalid substring range " + beginIndex + ',' + endIndex);
        }
        if (beginIndex == endIndex) {
            if (attributes.isEmpty())
                return;
            throw new IllegalArgumentException("Can't add attribute to 0-length text" + attributes);
        }

        // make sure we have run attribute data vectors
        if (runCount == 0) {
            createRunAttributeDataVectors();
        }

        // break up runs if necessary
        int beginRunIndex = ensureRunBreak(beginIndex);
        int endRunIndex = ensureRunBreak(endIndex);

        Iterator<? extends Map.Entry<? extends Attribute, ?>> iterator =
            attributes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<? extends Attribute, ?> entry = iterator.next();
            addAttributeRunData(entry.getKey(), entry.getValue(), beginRunIndex, endRunIndex);
        }
    }

    private synchronized void addAttributeImpl(Attribute attribute, Object value,
            int beginIndex, int endIndex) {

        // make sure we have run attribute data vectors
        if (runCount == 0) {
            createRunAttributeDataVectors();
        }

        // break up runs if necessary
        int beginRunIndex = ensureRunBreak(beginIndex);
        int endRunIndex = ensureRunBreak(endIndex);

        addAttributeRunData(attribute, value, beginRunIndex, endRunIndex);
    }

    private final void createRunAttributeDataVectors() {
        // use temporary variables so things remain consistent in case of an exception
        int newRunStarts[] = new int[ARRAY_SIZE_INCREMENT];

        @SuppressWarnings("unchecked")
        ArrayList<Attribute> newRunAttributes[] = (ArrayList<Attribute>[]) new ArrayList<?>[ARRAY_SIZE_INCREMENT];

        @SuppressWarnings("unchecked")
        ArrayList<Object> newRunAttributeValues[] = (ArrayList<Object>[]) new ArrayList<?>[ARRAY_SIZE_INCREMENT];

        runStarts = newRunStarts;
        runAttributes = newRunAttributes;
        runAttributeValues = newRunAttributeValues;
        runArraySize = ARRAY_SIZE_INCREMENT;
        runCount = 1; // assume initial run starting at index 0
    }

    // ensure there's a run break at offset, return the index of the run
    private final int ensureRunBreak(int offset) {
        return ensureRunBreak(offset, true);
    }

    /**
     * Ensures there is a run break at offset, returning the index of
     * the run. If this results in splitting a run, two things can happen:
     * <ul>
     * <li>If copyAttrs is true, the attributes from the existing run
     *     will be placed in both of the newly created runs.
     * <li>If copyAttrs is false, the attributes from the existing run
     * will NOT be copied to the run to the right (>= offset) of the break,
     * but will exist on the run to the left (< offset).
     * </ul>
     */
    private final int ensureRunBreak(int offset, boolean copyAttrs) {
        if (offset == length()) {
            return runCount;
        }

        // search for the run index where this offset should be
        int runIndex = 0;
        while (runIndex < runCount && runStarts[runIndex] < offset) {
            runIndex++;
        }

        // if the offset is at a run start already, we're done
        if (runIndex < runCount && runStarts[runIndex] == offset) {
            return runIndex;
        }

        // we'll have to break up a run
        // first, make sure we have enough space in our arrays
        if (runCount == runArraySize) {
            int newArraySize = runArraySize + ARRAY_SIZE_INCREMENT;
            int newRunStarts[] = new int[newArraySize];

            @SuppressWarnings("unchecked")
            ArrayList<Attribute> newRunAttributes[] = (ArrayList<Attribute>[]) new ArrayList<?>[newArraySize];

            @SuppressWarnings("unchecked")
            ArrayList<Object> newRunAttributeValues[] = (ArrayList<Object>[]) new ArrayList<?>[newArraySize];

            for (int i = 0; i < runArraySize; i++) {
                newRunStarts[i] = runStarts[i];
                newRunAttributes[i] = runAttributes[i];
                newRunAttributeValues[i] = runAttributeValues[i];
            }
            runStarts = newRunStarts;
            runAttributes = newRunAttributes;
            runAttributeValues = newRunAttributeValues;
            runArraySize = newArraySize;
        }

        // make copies of the attribute information of the old run that the new one used to be part of
        // use temporary variables so things remain consistent in case of an exception
        ArrayList<Attribute> newRunAttributes = null;
        ArrayList<Object> newRunAttributeValues = null;

        if (copyAttrs) {
            int rim1 = runIndex - 1;
            ArrayList<Attribute> oldRunAttributes = runAttributes[rim1];
            if (oldRunAttributes != null) {
                newRunAttributes = new ArrayList<>(oldRunAttributes);
            }
            ArrayList<Object> oldRunAttributeValues = runAttributeValues[rim1];
            if (oldRunAttributeValues != null) {
                newRunAttributeValues =  new ArrayList<>(oldRunAttributeValues);
            }
        }

        // now actually break up the run
        runCount++;
        for (int i = runCount - 1, j = i - 1; i > runIndex; i--, j--) {
            runStarts[i] = runStarts[j];
            runAttributes[i] = runAttributes[j];
            runAttributeValues[i] = runAttributeValues[j];
        }
        runStarts[runIndex] = offset;
        runAttributes[runIndex] = newRunAttributes;
        runAttributeValues[runIndex] = newRunAttributeValues;

        return runIndex;
    }

    // add the attribute attribute/value to all runs where beginRunIndex <= runIndex < endRunIndex
    private void addAttributeRunData(Attribute attribute, Object value,
            int beginRunIndex, int endRunIndex) {

        for (int i = beginRunIndex; i < endRunIndex; i++) {
            int keyValueIndex = -1; // index of key and value in our vectors; assume we don't have an entry yet
           ArrayList<Attribute> runAttribute = runAttributes[i];
           ArrayList<Object> runAttributeValue = runAttributeValues[i];
           if (runAttribute == null) {
                runAttributes[i] = runAttribute = new ArrayList<>();
                runAttributeValues[i] = runAttributeValue = new ArrayList<>();
            } else {
                // check whether we have an entry already
                keyValueIndex = runAttribute.indexOf(attribute);
            }

            if (keyValueIndex == -1) {
                // create new entry
                runAttribute.add(attribute);
                runAttributeValue.add(value);
            } else {
                // update existing entry
                runAttributeValue.set(keyValueIndex, value);
            }
        }
    }

    /**
     * Creates an AttributedCharacterIterator instance that provides access to the entire contents of
     * this string.
     *
     * @return An iterator providing access to the text and its attributes.
     */
    public AttributedCharacterIterator getIterator() {
      return new AttributedStringIterator(0, length());
    }

    // all (with the exception of length) reading operations are private,
    // since AttributedString instances are accessed through iterators.

    // length is package private so that CharacterIteratorFieldDelegate can
    // access it without creating an AttributedCharacterIterator.
    private int length() {
        return text.length();
    }

    private char charAt(int index) {
        return text.charAt(index);
    }

    @Nullable
    private synchronized Object getAttribute(Attribute attribute, int runIndex) {
        ArrayList<Attribute> currentRunAttributes = runAttributes[runIndex];
        ArrayList<Object> currentRunAttributeValues = runAttributeValues[runIndex];
        if (currentRunAttributes == null) {
            return null;
        }
        int attributeIndex = currentRunAttributes.indexOf(attribute);
        if (attributeIndex != -1) {
            return currentRunAttributeValues.get(attributeIndex);
        }
        else {
            return null;
        }
    }

    // gets an attribute value, but returns an annotation only if it's range does not extend outside the range beginIndex..endIndex
    @Nullable
    private Object getAttributeCheckRange(Attribute attribute, int runIndex, int beginIndex, int endIndex) {
        Object value = getAttribute(attribute, runIndex);
        if (value instanceof Annotation) {
            // need to check whether the annotation's range extends outside the iterator's range
            if (beginIndex > 0) {
                int currIndex = runIndex;
                int runStart = runStarts[currIndex];
                while (runStart >= beginIndex &&
                        valuesMatch(value, getAttribute(attribute, currIndex - 1))) {
                    currIndex--;
                    runStart = runStarts[currIndex];
                }
                if (runStart < beginIndex) {
                    // annotation's range starts before iterator's range
                    return null;
                }
            }
            int textLength = length();
            if (endIndex < textLength) {
                int currIndex = runIndex;
                int runLimit = (currIndex < runCount - 1) ? runStarts[currIndex + 1] : textLength;
                while (runLimit <= endIndex &&
                        valuesMatch(value, getAttribute(attribute, currIndex + 1))) {
                    currIndex++;
                    runLimit = (currIndex < runCount - 1) ? runStarts[currIndex + 1] : textLength;
                }
                if (runLimit > endIndex) {
                    // annotation's range ends after iterator's range
                    return null;
                }
            }
            // annotation's range is subrange of iterator's range,
            // so we can return the value
        }
        return value;
    }

    // returns whether all specified attributes have equal values in the runs with the given indices
    private boolean attributeValuesMatch(Set<? extends Attribute> attributes, int runIndex1, int runIndex2) {
        Iterator<? extends Attribute> iterator = attributes.iterator();
        while (iterator.hasNext()) {
            Attribute key = iterator.next();
           if (!valuesMatch(getAttribute(key, runIndex1), getAttribute(key, runIndex2))) {
                return false;
            }
        }
        return true;
    }

    // returns whether the two objects are either both null or equal
    private final static boolean valuesMatch(Object value1, Object value2) {
        if (value1 == null) {
            return value2 == null;
        } else {
            return value1.equals(value2);
        }
    }

    /**
     * Appends the contents of the CharacterIterator iterator into the
     * StringBuffer buf.
     */
    private final void appendContents(StringBuilder buf,
                                      CharacterIterator iterator) {
        int index = iterator.getBeginIndex();
        int end = iterator.getEndIndex();

        while (index < end) {
            iterator.setIndex(index++);
            buf.append(iterator.current());
        }
    }

    /**
     * Sets the attributes for the range from offset to the next run break
     * (typically the end of the text) to the ones specified in attrs.
     * This is only meant to be called from the constructor!
     */
    private void setAttributes(Map<Attribute, Object> attrs, int offset) {
        if (runCount == 0) {
            createRunAttributeDataVectors();
        }

        int index = ensureRunBreak(offset, false);
        int size;

        if (attrs != null && (size = attrs.size()) > 0) {
            ArrayList<Attribute> runAttrs = new ArrayList<>(size);
            ArrayList<Object> runValues = new ArrayList<>(size);
            Iterator<Map.Entry<Attribute, Object>> iterator = attrs.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Attribute, Object> entry = iterator.next();

                runAttrs.add(entry.getKey());
                runValues.add(entry.getValue());
            }
            runAttributes[index] = runAttrs;
            runAttributeValues[index] = runValues;
        }
    }

    /**
     * Returns true if the attributes specified in last and attrs differ.
     */
    private static <K,V> boolean mapsDiffer(Map<K, V> last, Map<K, V> attrs) {
        if (last == null) {
            return (attrs != null && attrs.size() > 0);
        }
        return (!last.equals(attrs));
    }


    // the iterator class associated with this string class

    final private class AttributedStringIterator implements AttributedCharacterIterator {

        // note on synchronization:
        // we don't synchronize on the iterator, assuming that an iterator is only used in one thread.
        // we do synchronize access to the AttributedString however, since it's more likely to be shared between threads.

        // start and end index for our iteration
        private int beginIndex;
        private int endIndex;

        // the current index for our iteration
        // invariant: beginIndex <= currentIndex <= endIndex
        private int currentIndex;

        // information about the run that includes currentIndex
        private int currentRunIndex;
        private int currentRunStart;
        private int currentRunLimit;

        // constructor
        AttributedStringIterator(int beginIndex, int endIndex) {

            if (beginIndex < 0 || beginIndex > endIndex || endIndex > length()) {
                throw new IllegalArgumentException("Invalid substring range " + beginIndex + ',' + endIndex);
            }

            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
            this.currentIndex = beginIndex;
            updateRunInfo();
        }

        // Object methods. See documentation in that class.

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AttributedStringIterator)) {
                return false;
            }

            AttributedStringIterator that = (AttributedStringIterator) obj;

            if (AttributedString.this != that.getString())
                return false;
            return !(currentIndex != that.currentIndex || beginIndex != that.beginIndex || endIndex != that.endIndex);
        }

        public int hashCode() {
            return text.hashCode() ^ currentIndex ^ beginIndex ^ endIndex;
        }

        public AttributedStringIterator clone() {
            try {
                return (AttributedStringIterator) super.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }

        // CharacterIterator methods. See documentation in that interface.

        public char first() {
            return internalSetIndex(beginIndex);
        }

        public char last() {
            if (endIndex == beginIndex) {
                return internalSetIndex(endIndex);
            } else {
                return internalSetIndex(endIndex - 1);
            }
        }

        public char current() {
            if (currentIndex == endIndex) {
                return DONE;
            } else {
                return charAt(currentIndex);
            }
        }

        public char next() {
            if (currentIndex < endIndex) {
                return internalSetIndex(currentIndex + 1);
            }
            else {
                return DONE;
            }
        }

        public char previous() {
            if (currentIndex > beginIndex) {
                return internalSetIndex(currentIndex - 1);
            }
            else {
                return DONE;
            }
        }

        public char setIndex(int position) {
            if (position < beginIndex || position > endIndex)
                throw new IllegalArgumentException("Invalid index " + position);
            return internalSetIndex(position);
        }

        public int getBeginIndex() {
            return beginIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public int getIndex() {
            return currentIndex;
        }

        // AttributedCharacterIterator methods. See documentation in that interface.

        public int getRunStart() {
            return currentRunStart;
        }

        public int getRunStart(Attribute attribute) {
            if (currentRunStart == beginIndex || currentRunIndex == -1) {
                return currentRunStart;
            } else {
                Object value = getAttribute(attribute);
                int runStart = currentRunStart;
                int runIndex = currentRunIndex;
                while (runStart > beginIndex &&
                        valuesMatch(value, AttributedString.this.getAttribute(attribute, runIndex - 1))) {
                    runIndex--;
                    runStart = runStarts[runIndex];
                }
                if (runStart < beginIndex) {
                    runStart = beginIndex;
                }
                return runStart;
            }
        }

        public int getRunStart(Set<? extends Attribute> attributes) {
            if (currentRunStart == beginIndex || currentRunIndex == -1) {
                return currentRunStart;
            } else {
                int runStart = currentRunStart;
                int runIndex = currentRunIndex;
                while (runStart > beginIndex &&
                        AttributedString.this.attributeValuesMatch(attributes, currentRunIndex, runIndex - 1)) {
                    runIndex--;
                    runStart = runStarts[runIndex];
                }
                if (runStart < beginIndex) {
                    runStart = beginIndex;
                }
                return runStart;
            }
        }

        public int getRunLimit() {
            return currentRunLimit;
        }

        public int getRunLimit(Attribute attribute) {
            if (currentRunLimit == endIndex || currentRunIndex == -1) {
                return currentRunLimit;
            } else {
                Object value = getAttribute(attribute);
                int runLimit = currentRunLimit;
                int runIndex = currentRunIndex;
                while (runLimit < endIndex &&
                        valuesMatch(value, AttributedString.this.getAttribute(attribute, runIndex + 1))) {
                    runIndex++;
                    runLimit = runIndex < runCount - 1 ? runStarts[runIndex + 1] : endIndex;
                }
                if (runLimit > endIndex) {
                    runLimit = endIndex;
                }
                return runLimit;
            }
        }

        public int getRunLimit(Set<? extends Attribute> attributes) {
            if (currentRunLimit == endIndex || currentRunIndex == -1) {
                return currentRunLimit;
            } else {
                int runLimit = currentRunLimit;
                int runIndex = currentRunIndex;
                while (runLimit < endIndex &&
                        AttributedString.this.attributeValuesMatch(attributes, currentRunIndex, runIndex + 1)) {
                    runIndex++;
                    runLimit = runIndex < runCount - 1 ? runStarts[runIndex + 1] : endIndex;
                }
                if (runLimit > endIndex) {
                    runLimit = endIndex;
                }
                return runLimit;
            }
        }

        @SuppressFBWarnings("SEO_SUBOPTIMAL_EXPRESSION_ORDER") // looks like a FP
        public Map<Attribute,Object> getAttributes() {
            if (runAttributes == null || currentRunIndex == -1 || runAttributes[currentRunIndex] == null) {
                // ??? would be nice to return null, but current spec doesn't allow it
                // returning Hashtable saves AttributeMap from dealing with emptiness
                return new Hashtable<>();
            }
            return new AttributeMap(currentRunIndex, beginIndex, endIndex);
        }

        public Set<Attribute> getAllAttributeKeys() {
            // ??? This should screen out attribute keys that aren't relevant to the client
            if (runAttributes == null) {
                // ??? would be nice to return null, but current spec doesn't allow it
                // returning HashSet saves us from dealing with emptiness
                return new HashSet<>();
            }
            synchronized (AttributedString.this) {
                // ??? should try to create this only once, then update if necessary,
                // and give callers read-only view
                Set<Attribute> keys = new HashSet<>();
                int i = 0;
                while (i < runCount) {
                    if (runStarts[i] < endIndex && (i == runCount - 1 || runStarts[i + 1] > beginIndex)) {
                        ArrayList<Attribute> currentRunAttributes = runAttributes[i];
                        if (currentRunAttributes != null) {
                            int j = currentRunAttributes.size();
                            while (j-- > 0) {
                                keys.add(currentRunAttributes.get(j));
                            }
                        }
                    }
                    i++;
                }
                return keys;
            }
        }

        @Nullable
        public Object getAttribute(Attribute attribute) {
            int runIndex = currentRunIndex;
            if (runIndex < 0) {
                return null;
            }
            return AttributedString.this.getAttributeCheckRange(attribute, runIndex, beginIndex, endIndex);
        }

        // internally used methods

        private AttributedString getString() {
            return AttributedString.this;
        }

        // set the current index, update information about the current run if necessary,
        // return the character at the current index
        private char internalSetIndex(int position) {
            currentIndex = position;
            if (position < currentRunStart || position >= currentRunLimit) {
                updateRunInfo();
            }
            if (currentIndex == endIndex) {
                return DONE;
            } else {
                return charAt(position);
            }
        }

        // update the information about the current run
        private void updateRunInfo() {
            if (currentIndex == endIndex) {
                currentRunStart = currentRunLimit = endIndex;
                currentRunIndex = -1;
            } else {
                synchronized (AttributedString.this) {
                    int runIndex = -1;
                    while (runIndex < runCount - 1 && runStarts[runIndex + 1] <= currentIndex)
                        runIndex++;
                    currentRunIndex = runIndex;
                    if (runIndex >= 0) {
                        currentRunStart = runStarts[runIndex];
                        if (currentRunStart < beginIndex)
                            currentRunStart = beginIndex;
                    }
                    else {
                        currentRunStart = beginIndex;
                    }
                    if (runIndex < runCount - 1) {
                        currentRunLimit = runStarts[runIndex + 1];
                        if (currentRunLimit > endIndex)
                            currentRunLimit = endIndex;
                    }
                    else {
                        currentRunLimit = endIndex;
                    }
                }
            }
        }

    }

    // the map class associated with this string class, giving access to the attributes of one run

    final private class AttributeMap extends AbstractMap<Attribute,Object> {

        final int runIndex;
        final int beginIndex;
        final int endIndex;

        AttributeMap(int runIndex, int beginIndex, int endIndex) {
            this.runIndex = runIndex;
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }

        public Set<Map.Entry<Attribute, Object>> entrySet() {
            HashSet<Map.Entry<Attribute, Object>> set = new HashSet<>();
            synchronized (AttributedString.this) {
                int size = runAttributes[runIndex].size();
                for (int i = 0; i < size; i++) {
                    Attribute key = runAttributes[runIndex].get(i);
                    Object value = runAttributeValues[runIndex].get(i);
                    if (value instanceof Annotation) {
                        value = AttributedString.this.getAttributeCheckRange(key,
                                                             runIndex, beginIndex, endIndex);
                        if (value == null) {
                            continue;
                        }
                    }

                    Map.Entry<Attribute, Object> entry = new AttributeEntry(key, value);
                    set.add(entry);
                }
            }
            return set;
        }

        @Nullable
        public Object get(Object key) {
            return AttributedString.this.getAttributeCheckRange((Attribute) key, runIndex, beginIndex, endIndex);
        }
    }

  @Override
  public String toString() {
    return "AttributedString{" + "text=" + text + ", runArraySize=" + runArraySize
            + ", runCount=" + runCount + ", runStarts=" + Arrays.toString(runStarts) + ", runAttributes="
            + Arrays.toString(runAttributes) + ", runAttributeValues=" + Arrays.toString(runAttributeValues) + '}';
  }



}

final class AttributeEntry implements Map.Entry<Attribute,Object> {

    private final Attribute key;
    private final Object value;

    AttributeEntry(Attribute key, Object value) {
        this.key = key;
        this.value = value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AttributeEntry)) {
            return false;
        }
        AttributeEntry other = (AttributeEntry) o;
        return other.key.equals(key) && Objects.equals(value, other.value);
    }

    public Attribute getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Object setValue(Object newValue) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        return key.hashCode() ^ (value==null ? 0 : value.hashCode());
    }

    public String toString() {
        return key.toString() + '=' + value;
    }
}
