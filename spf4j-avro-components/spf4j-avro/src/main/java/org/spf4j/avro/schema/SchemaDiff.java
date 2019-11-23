/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.avro.schema;

import org.apache.avro.Schema;

/**
 *
 * @author Zoltan Farkas
 */
public interface SchemaDiff {

  enum Type {
    DIFFERENT_TYPES,
    DIFFERENT_FIXED_SIZE,
    DIFFERENT_NAMES,
    DIFFERENT_ENUM_VALUES,
    SCHEMA_MISSING_LEFT,
    SCHEMA_MISSING_RIGHT,
    FIELD_MISSING_LEFT,
    FIELD_MISSING_RIGHT,
    DIFFERENT_LOGICAL_TYPES,
    DIFFERENT_SCHEMA_PROPERTIES,
    DIFFERRENT_SCHEMA_DOC,
    DIFFERENT_FIELD_DEFAULTS,
    DIFFERENT_FIELD_PROPERTIES,
    DIFFERRENT_FIELD_DOC
  }

  String getPath();

  Type getDiffType();

  Schema getLeft();

  Schema getRight();

  Schema.Field getLeftField();

  Schema.Field getRightField();

  static SchemaDiff of(String path, Schema left, Schema right, Type type) {
    return new SchemaDiffImpl(path, type, left, right);
  }

  static SchemaDiff of(String path, Schema.Field left, Schema.Field right, Type type) {
    return new SchemaFieldDiffImpl(path, type, left, right);
  }

   static class SchemaDiffImpl implements SchemaDiff {

    private final String path;
    private final Type type;
    private final Schema left;
    private final Schema right;

    public SchemaDiffImpl(String path, Type type, Schema left, Schema right) {
      this.path = path;
      this.type = type;
      this.left = left;
      this.right = right;
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public Type getDiffType() {
      return type;
    }

    @Override
    public Schema getLeft() {
      return left;
    }

    @Override
    public Schema getRight() {
      return right;
    }

    @Override
    public Schema.Field getLeftField() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema.Field getRightField() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "SchemaDiff{" + "path=" + path + ", type=" + type
              + (left != null ?  ", left=" + left.getFullName() : "")
              + (right != null ? ", right=" + right.getFullName() : "")
              + '}';
    }



  }

  static class SchemaFieldDiffImpl implements SchemaDiff {

    private final String path;
    private final Type type;
    private final Schema.Field left;
    private final Schema.Field right;

    public SchemaFieldDiffImpl(String path, Type type, Schema.Field left, Schema.Field right) {
      this.path = path;
      this.type = type;
      this.left = left;
      this.right = right;
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public Type getDiffType() {
      return type;
    }

    @Override
    public Schema getLeft() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema getRight() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Schema.Field getLeftField() {
      return left;
    }

    @Override
    public Schema.Field getRightField() {
      return right;
    }

    @Override
    public String toString() {
      return "SchemaFieldDiff{" + "path=" + path + ", type=" + type
              + (left != null ? ", left=" + left : "")
              + (right != null ? ", right=" + right : "")
              + '}';
    }

  }

}
