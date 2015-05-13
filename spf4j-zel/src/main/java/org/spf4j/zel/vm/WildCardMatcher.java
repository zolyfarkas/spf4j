/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

/**
 *
 * @author zoly
 */
public final class WildCardMatcher {

    public WildCardMatcher(final String wildcardExp) {
        this.wildcardExp = wildcardExp;
    }

    private final String wildcardExp;

    /**
     * Returns true if a matches b and false otherwise
     *
     * @param o Object
     * @return boolean
     */
    public boolean match(final String str) {
        return match(this.wildcardExp, str);
    }

    /**
     * static wildcard matching
     *
     * @param wildcard
     * @param v
     * @return return true ifr string v matches the wildcard
     */
    @SuppressWarnings("empty-statement")
    public static boolean match(final String wildcard, final String v) {
        int i = 0;
        int j = 0;
        final int length = wildcard.length();
        for (; i < length; i++, j++) {
            final char some2 = wildcard.charAt(i);
            if (some2 != v.charAt(j)) {
                if (some2 == '*') {
                    i++;
                    if (i == length) {
                        return true;
                    }
                    final char some = wildcard.charAt(i);
                    while (some != v.charAt(j)) {
                        ++j;
                    }
                    j--;
                } else if (some2 != '?') {
                    return false;
                }
            }
        }
        return (j == v.length());
    }

    /**
     * return the java regexp version of this class
     *
     * @return
     */
    public String getJavaRegexp() {
        final StringBuilder buff = new StringBuilder();
        final int length = wildcardExp.length();
        for (int i = 0; i < length; i++) {
            final char c = wildcardExp.charAt(i);
            switch (c) {
                case '*':
                    buff.append("[^\\[\\]\\.]+");
                    break;
                case '?':
                    buff.append('.');
                    break;
                case '[':
                case ']':
                case '.':
                    buff.append('\\').append(c);
                    break;
                default:
                    buff.append(c);
            }
        }
        return buff.toString();
    }

    /**
     * return if this variable object is a exact mach or a set of variables I use a for cycle to iterate through strings
     * using a CharacterIterator migh tbe better ?
     *
     * @param str - string to test
     * @return true if str is a wildcard matcher
     */
    public static boolean isWildcardMatcher(final String str) {
        final int length = str.length();
        for (int i = 0; i < length; i++) {
            final char c = str.charAt(i);
            if (c == '*' || c == '?') {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder().append('`').append(wildcardExp).append('`').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WildCardMatcher other = (WildCardMatcher) obj;
        return !((this.wildcardExp == null)
                ? (other.wildcardExp != null) : !this.wildcardExp.equals(other.wildcardExp));
    }



    @Override
    public int hashCode() {
        int hash = 5;
        return 43 * hash + (this.wildcardExp != null ? this.wildcardExp.hashCode() : 0);
    }
}
