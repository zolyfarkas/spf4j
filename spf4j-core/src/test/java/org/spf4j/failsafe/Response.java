/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.failsafe;

import java.util.Objects;

/**
 *
 * @author Zoltan Farkas
 */
public final class Response {

  enum Type {
    OK, REDIRECT, RETRY_LATER, TRANSIENT_ERROR, CLIENT_ERROR, ERROR
  }

  private final Type type;

  private final Object payload;

  public Response(final Type type, final Object payload) {
    this.type = type;
    this.payload = payload;
  }

  public Type getType() {
    return type;
  }

  public Object getPayload() {
    return payload;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 43 * hash + Objects.hashCode(this.type);
    return 43 * hash + Objects.hashCode(this.payload);
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
    final Response other = (Response) obj;
    if (this.type != other.type) {
      return false;
    }
    return Objects.equals(this.payload, other.payload);
  }

  @Override
  public String toString() {
    return "Response{" + "type=" + type + ", payload=" + payload + '}';
  }

}
