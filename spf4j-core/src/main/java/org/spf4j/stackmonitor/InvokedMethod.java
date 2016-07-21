package org.spf4j.stackmonitor;

import java.io.Serializable;
import java.util.Objects;
import org.spf4j.base.Method;

/**
 *
 * @author zoly
 */
public final class InvokedMethod implements Serializable {

  private static final long serialVersionUID = 1L;

  private final int invocationId;

  private final Method method;

  public InvokedMethod(final Method method, final int invocationId) {
    this.method = method;
    this.invocationId = invocationId;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 47 * hash + this.invocationId;
    return 47 * hash + Objects.hashCode(this.method);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final InvokedMethod other = (InvokedMethod) obj;
    if (!Objects.equals(this.method, other.method)) {
      return false;
    }
    return this.invocationId == other.invocationId;
  }

  public Method getMethod() {
    return method;
  }

  public InvokedMethod withId(final int pid) {
    return new InvokedMethod(this.method, pid);
  }

  public InvokedMethod withNewId() {
    return new InvokedMethod(this.method, invocationId + 1);
  }

  public static final InvokedMethod ROOT = new InvokedMethod(Method.ROOT, 0);

  @Override
  public String toString() {
    return "InvokedMethod{" + "invocationId=" + invocationId + ", method=" + method + '}';
  }

}
