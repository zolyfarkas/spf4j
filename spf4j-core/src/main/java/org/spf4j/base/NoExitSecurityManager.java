package org.spf4j.base;

import java.security.Permission;

/**
 * @author zoly
 */
public final class NoExitSecurityManager extends SecurityManager {

  @Override
  public void checkPermission(Permission perm) {
    // allow anything.
  }

  @Override
  public void checkPermission(Permission perm, Object context) {
    // allow anything.
  }

  @Override
  public void checkExit(int status) {
    super.checkExit(status);
    throw new ExitException(status);
  }

}
