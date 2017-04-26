
package org.spf4j.base;

import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class JavaUtils {

  private JavaUtils() { }


  public static boolean isJavaIdentifier(@Nullable final CharSequence cs) {
    if (cs == null) {
      return false;
    }
    final int length = cs.length();
    if (length <= 0) {
      return false;
    }
    if (!Character.isJavaIdentifierStart(cs.charAt(0))) {
      return false;
    }
    for (int i = 1; i < length; i++) {
      if (!Character.isJavaIdentifierPart(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }



}
