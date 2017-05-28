
package org.spf4j.base;

import java.lang.ref.SoftReference;

/**
 *
 * @author zoly
 */
public final class TLScratch {

  private static final ThreadLocal<SoftReference<byte[]>> BYTES_TMP = new ThreadLocal<>();

  private static final ThreadLocal<SoftReference<char[]>> CHARS_TMP = new ThreadLocal<>();
  
  private TLScratch() { }

  /**
   * returns a thread local byte array of at least the size requested. use only for temporary purpose. This method needs
   * to be carefully used!
   *
   * @param size - the minimum size of the temporary buffer requested.
   * @return - the temporary buffer.
   */
  public static byte[] getBytesTmp(final int size) {
    SoftReference<byte[]> sr = BYTES_TMP.get();
    byte[] result;
    if (sr == null) {
      result = new byte[size];
      BYTES_TMP.set(new SoftReference<>(result));
    } else {
      result = sr.get();
      if (result == null || result.length < size) {
        result = new byte[size];
        BYTES_TMP.set(new SoftReference<>(result));
      }
    }
    return result;
  }

  /**
   * returns a thread local char array of at least the requested size. Use only for temporary purpose.
   *
   * @param size - the minimum size of the temporary buffer requested.
   * @return - the temporary buffer.
   */
  public static char[] getCharsTmp(final int size) {
    SoftReference<char[]> sr = CHARS_TMP.get();
    char[] result;
    if (sr == null) {
      result = new char[size];
      CHARS_TMP.set(new SoftReference<>(result));
    } else {
      result = sr.get();
      if (result == null || result.length < size) {
        result = new char[size];
        CHARS_TMP.set(new SoftReference<>(result));
      }
    }
    return result;
  }

}
