/*
 * Copyright 2020 SPF4J.
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
package org.apache.avro;

import com.fasterxml.jackson.core.JsonParser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.apache.avro.Schema.FACTORY;

/**
 *
 * @author Zoltan Farkas
 */
public class ExtendedParser {

  public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

  private static final ThreadLocal<Boolean> ALLOW_UNDEF_LT = new ThreadLocal<>();


  private final ExtendedNames names;
  private boolean validate = true;
  private boolean validateDefaults = false;

  public static boolean isAllowUndefinedLogicalTypes() {
    Boolean tlb = ALLOW_UNDEF_LT.get();
    if (tlb == null) {
      return Boolean.getBoolean("allowUndefinedLogicalTypes");
    } else {
      return tlb;
    }
  }

  public static void setAllowUndefinedLogicalTypesThreadLocal(final Boolean isAllowUndefinedLogicalTypes) {
    ALLOW_UNDEF_LT.set(isAllowUndefinedLogicalTypes);
  }

  public static Boolean getAllowUndefinedLogicalTypesThreadLocal() {
    return ALLOW_UNDEF_LT.get();
  }

  public ExtendedParser(final ExtendedNames names) {
    this.names = names;
  }

  public ExtendedParser() {
    SchemaResolver resolver = SchemaResolvers.getDefault();
    names = new ExtendedAvroNamesRefResolver(resolver);
  }

  /**
   * Adds the provided types to the set of defined, named types known to this parser.
   */
  public ExtendedParser addTypes(Map<String, Schema> types) {
    for (Schema s : types.values()) {
      names.add(s);
    }
    return this;
  }

  /**
   * Returns the set of defined, named types known to this parser.
   */
  public Map<String, Schema> getTypes() {
    Collection<Schema> values = names.values();
    Map<String, Schema> result = newLinkedHashMapWithExpectedSize(values.size());
    for (Schema s : values) {
      result.put(s.getFullName(), s);
    }
    return result;
  }

  /**
   * Enable or disable name validation.
   */
  public ExtendedParser setValidssate(boolean validate) {
    this.validate = validate;
    return this;
  }

  /**
   * True iff names are validated. True by default.
   */
  public boolean getValidate() {
    return this.validate;
  }

  /**
   * Enable or disable default value validation.
   */
  public ExtendedParser setValidateDefaults(boolean validateDefaults) {
    this.validateDefaults = validateDefaults;
    return this;
  }

  /**
   * True iff default values are validated. False by default.
   */
  public boolean getValidateDefaults() {
    return this.validateDefaults;
  }

  /**
   * Parse a schema from the provided file. If named, the schema is added to the names known to this parser.
   */
  public Schema parse(File file) throws IOException {
    return parse(FACTORY.createParser(file));
  }

  public Schema parse(InputStream in) throws IOException {
    return parse(in, isAllowUndefinedLogicalTypes());
  }

  /**
   * Parse a schema from the provided stream. If named, the schema is added to the names known to this parser.
   */
  public Schema parse(InputStream in, final boolean allowUndefinedLogicalTypes) throws IOException {
    return parse(FACTORY.createParser(in), allowUndefinedLogicalTypes);
  }

  /**
   * Read a schema from one or more json strings
   */
  public Schema parse(String s, String... more) {
    StringBuilder b = new StringBuilder(s.length() * (more.length + 1));
    b.append(s);
    for (String part : more) {
      b.append(part);
    }
    return parse(b.toString());
  }

  public Schema parse(String s) {
    return parse(s, isAllowUndefinedLogicalTypes());
  }

  /**
   * Parse a schema from the provided string. If named, the schema is added to the names known to this parser.
   */
  public Schema parse(String s, final boolean allowUndefinedLogicalTypes) {
    try {
      return parse(FACTORY.createParser(new StringReader(s)), allowUndefinedLogicalTypes);
    } catch (IOException e) {
      throw new SchemaParseException(e);
    }
  }

  public Schema parse(JsonParser parser) throws IOException {
    return parse(parser, isAllowUndefinedLogicalTypes());
  }

  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
  public Schema parse(JsonParser parser, final boolean allowUndefinedLogicalTypes) throws IOException {
    return SchemaAdapter.parse(parser, names, allowUndefinedLogicalTypes, validate, validateDefaults);
  }

  public static <K, V> LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
    return new LinkedHashMap<K, V>(capacity(expectedSize));
  }

  static int capacity(int expectedSize) {
    if (expectedSize < 3) {
      if (expectedSize < 0) {
        throw new IllegalArgumentException("Invalid capacity: " + expectedSize);
      }
      return expectedSize + 1;
    }
    if (expectedSize < MAX_POWER_OF_TWO) {
      // This is the calculation used in JDK8 to resize when a putAll
      // happens; it seems to be the most conservative calculation we
      // can make.  0.75 is the default load factor.ss
      return (int) ((float) expectedSize / 0.75F + 1.0F);
    }
    return Integer.MAX_VALUE; // any large value
  }

  @Override
  public String toString() {
    return "ExtendedParser{" + "names=" + names + ", validate=" + validate
            + ", validateDefaults=" + validateDefaults + '}';
  }




}
