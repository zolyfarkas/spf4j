package org.spf4j.io;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

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

/**
 * Some standard mime types.
 * @author Zoltan Farkas
 */
public final class MimeTypes {

  public static final MimeType PLAIN_TEXT;
  public static final MimeType APPLICATION_JSON;
  public static final MimeType APPLICATION_OCTET_STREAM;

  static {
    try {
      PLAIN_TEXT = new MimeType("plain", "text");
      APPLICATION_JSON = new MimeType("application", "json");
      APPLICATION_OCTET_STREAM = new MimeType("application", "octet-stream");
    } catch (MimeTypeParseException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private MimeTypes() {
  }

}
