package org.spf4j.base;

import java.security.Permission;

/**
 * @author zoly
 */
public final class NoExitSecurityManager extends SecurityManager {

  @Override
  public void checkPermission(final Permission perm) {
    // allow anything.
  }

  @Override
  public void checkPermission(final Permission perm, final Object context) {
    // allow anything.
  }

  @Override
  public void checkExit(final int status) {
    super.checkExit(status);
    throw new ExitException(status);
  }

}
