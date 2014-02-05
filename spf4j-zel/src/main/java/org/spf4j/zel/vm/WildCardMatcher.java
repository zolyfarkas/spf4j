/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

/**
 *
 * @author zoly
 */
public class WildCardMatcher
{
    public WildCardMatcher(String wildcardExp)
    {
        this.wildcardExp = wildcardExp;
    }

    private String wildcardExp;


    /**
     * Returns true if a matches b and false otherwise
     *
     * @param o Object
     * @return boolean
     */
    public final boolean match(final String str)
    {
        return match(this.wildcardExp, str);
    }


    /**
     * static wildcard matching
     * @param wildcard
     * @param v
     * @return return true ifr string v matches the wildcard
     */
    @SuppressWarnings("empty-statement")
    public static final boolean match(final String wildcard, final String v)
    {
        int i = 0;
        int j = 0;
        final int length = wildcard.length();
        for (; i < length; i++, j++)
        {
            final char some2 = wildcard.charAt(i);
            if (some2 != v.charAt(j))
            {
                if (some2 == '*')
                {
                    i++;
                    if (i == length)
                    {
                        return true;
                    }
                    final char some = wildcard.charAt(i);
                    while (some != v.charAt(j))
                        ++j;
                    j--;
                } else if (some2 != '?')
                {
                    return false;
                }
            }
        }
        if (j != v.length())
        {
            return false;
        }
        return true;
    }

    /**
     * return the java regexp version of this class
     * @return
     */
    public String getJavaRegexp()
    {
        final StringBuilder buff = new StringBuilder();
        final int length = wildcardExp.length();
        for (int i = 0; i < length; i++)
        {
            final char c = wildcardExp.charAt(i);
            switch (c)
            {
                case '*':
                    buff.append("[^\\[\\]\\.]+");
                    break;
                case '?':
                    buff.append(".");
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
     * return if this variable object is a exact mach or a set of variables
     * I use a for cycle to iterate through strings
     * using a CharacterIterator migh tbe better ?
     *
     * @param str - string to test
     * @return true if str is a wildcard matcher
     */

    public static final boolean isWildcardMatcher(final String str)
    {
        final int length = str.length();
        for (int i = 0; i < length; i++)
        {
            final char c = str.charAt(i);
            if (c == '*' || c == '?')
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "`" + wildcardExp + "`";
    }

    @Override
    public boolean equals(Object o)
    {
      if (o == null)
          return false;
      else if (o instanceof String)
          return ((String)o).equals(this.wildcardExp);
      else if (o instanceof WildCardMatcher)
          return ((WildCardMatcher)o).wildcardExp.equals(this.wildcardExp);
      return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 43 * hash + (this.wildcardExp != null ? this.wildcardExp.hashCode() : 0);
        return hash;
    }
}
