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
package org.spf4j.maven.plugin.avro.avscp.validation;

import java.io.IOException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @author Zoltan Farkas
 */
public interface Validator<T> {

  class Result {

    private static final Result VALID = new Result(true, null, null);

    private final boolean isValid;
    private final String validationErrorMessage;
    private final Exception validationException;

    private Result(final boolean isValid, @Nullable final String validationErrorMessage,
            @Nullable final Exception validationException) {
      this.isValid = isValid;
      this.validationErrorMessage = validationErrorMessage;
      this.validationException = validationException;
    }

    public boolean isValid() {
      return isValid;
    }

    public boolean isFailed() {
      return !isValid;
    }

    public String getValidationErrorMessage() {
      return validationErrorMessage;
    }

    public Exception getValidationException() {
      return validationException;
    }

    public static Result valid() {
      return VALID;
    }

    public static Result failed(final String validationErrorMessage) {
      return new Result(false, validationErrorMessage, null);
    }

    public static Result failed(final String validationErrorMessage, final Exception ex) {
      return new Result(false, validationErrorMessage, ex);
    }

    public static Result failed(final Exception ex) {
      return new Result(false, ex.getMessage(), ex);
    }

  }

  @NonNull
  String getName();

  Class<T> getValidationInput();

  @NonNull
  @CheckReturnValue
  Result validate(T object) throws IOException;

}
