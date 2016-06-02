package org.spf4j.configDiscovery.maven.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nonnull;

/**
 *
 * @author zoly
 */
public final class JsonUtils {

  private JsonUtils() {
  }

  public static CharSequence toJsonString(final String str) {
    StringBuilder sb = new StringBuilder(str.length() + 4);
    sb.append('"');
    escapeJsonString(str, sb);
    sb.append('"');
    return sb;
  }

  public static void escapeJsonString(@Nonnull final String toEscape, final StringBuilder jsonString) {
    int len = toEscape.length();
    for (int i = 0; i < len; i++) {
      char c = toEscape.charAt(i);
      appendJsonStringEscapedChar(c, jsonString);
    }
  }

  public static void appendJsonStringEscapedChar(final char c, final StringBuilder jsonString) {
    switch (c) {
      case '\\':
      case '"':
        jsonString.append('\\');
        jsonString.append(c);
        break;
      case '\b':
        jsonString.append("\\b");
        break;
      case '\t':
        jsonString.append("\\t");
        break;
      case '\n':
        jsonString.append("\\n");
        break;
      case '\f':
        jsonString.append("\\f");
        break;
      case '\r':
        jsonString.append("\\r");
        break;
      default:
        if (c < ' ') {
          jsonString.append("\\u");
          appendUnsignedStringPadded(jsonString, (int) c, 4, 4);
        } else {
          jsonString.append(c);
        }
    }
  }

  public static void appendUnsignedStringPadded(final StringBuilder sb, final int nr, final int shift,
          final int padTo) {
    long i = nr;
    char[] buf = BUFF.get();
    int charPos = 32;
    int radix = 1 << shift;
    long mask = radix - 1;
    do {
      buf[--charPos] = DIGITS[(int) (i & mask)];
      i >>>= shift;
    } while (i != 0);
    final int nrChars = 32 - charPos;
    if (nrChars > padTo) {
      throw new IllegalArgumentException("Your pad to value " + padTo
              + " is to small, must be at least " + nrChars);
    } else {
      for (int j = 0, n = padTo - nrChars; j < n; j++) {
        sb.append('0');
      }
    }
    sb.append(buf, charPos, nrChars);
  }

  private static final ThreadLocal<char[]> BUFF = new ThreadLocal<char[]>() {

    @Override
    @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
    protected char[] initialValue() {
      return new char[64];
    }

  };

  private static final char[] DIGITS = {
    '0', '1', '2', '3', '4', '5',
    '6', '7', '8', '9', 'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h',
    'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z'
  };

}
