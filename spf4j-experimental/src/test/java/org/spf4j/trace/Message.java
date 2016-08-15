
package org.spf4j.trace;

import java.util.Map;

/**
 *
 * @author zoly
 */
public final class Message<P> {

  private final P payload;

  private final Map<String, String> headers;

  public Message(final P payload, Map<String, String> headers) {
    this.payload = payload;
    this.headers = headers;
  }

  public P getPayload() {
    return payload;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }


}
