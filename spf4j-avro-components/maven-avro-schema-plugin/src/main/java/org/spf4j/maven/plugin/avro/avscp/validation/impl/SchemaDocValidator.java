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
package org.spf4j.maven.plugin.avro.avscp.validation.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.spf4j.avro.schema.SchemaVisitor;
import org.spf4j.avro.schema.SchemaVisitorAction;
import org.spf4j.maven.plugin.avro.avscp.validation.Validator;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.CharSequences;
import org.spf4j.maven.plugin.avro.avscp.ValidatorMojo;

/**
 * Validates schema documentation fields are not empty for:
 * Records, Fixed, Enum, Record Fields.
 * Additionally for record fields of union type where one type is null, the doc if validated is it contain an
 * explanation for the meaning of the null value (null string present in doc field)
 *
 * @author Zoltan Farkas
 */
public final class SchemaDocValidator implements Validator<Schema> {

  @Override
  public String getName() {
    return "docValidator";
  }

  @Override
  @Nonnull
  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // not in this case
  public Result validate(final Schema schema, final ValidatorMojo mojo) {
    return Schemas.visit(schema, new DocValidatorVisitor(schema));
  }

  @Nullable
  private static Schema.Type getCollectionType(final Schema unionSchema) {
    for (Schema schema : unionSchema.getTypes()) {
      Schema.Type type = schema.getType();
      switch (type) {
        case ARRAY:
        case MAP:
        case STRING:
          return type;
        default:
        // skip
      }
    }
    return null;
  }

  @Override
  public Class<Schema> getValidationInput() {
    return Schema.class;
  }

  private static class DocValidatorVisitor implements SchemaVisitor<Result> {

    private final List<String> issues;

    private final Schema root;

    DocValidatorVisitor(final Schema root) {
      issues = new ArrayList<>(4);
      this.root = root;
    }

    @Override
    public SchemaVisitorAction visitTerminal(final Schema schema) {
      switch (schema.getType()) {
        case ENUM:
        case FIXED:
          String doc = schema.getDoc();
          if (doc == null || doc.trim().isEmpty()) {
            issues.add("Please document " + schema.getFullName());
          }
          break;
        default:
      }
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    public SchemaVisitorAction visitNonTerminal(final Schema schema) {
      if (schema.getType() == Schema.Type.RECORD) {
        String doc = schema.getDoc();
        if (doc == null || doc.trim().isEmpty()) {
          issues.add("Please document " + schema.getFullName());
        }
        for (Field field : schema.getFields()) {
          doc = field.doc();
          if (doc == null || doc.trim().isEmpty()) {
            String errText = "Please document " + field.name() + '@' + schema.getFullName();
            String source = schema.getProp("sourceIdl");
            if (source != null) {
              errText += " from " + source;
            }
            issues.add(errText);
          } else {
            Schema fs = field.schema();
            if (Schemas.isNullableUnion(fs) && !CharSequences.containsIgnoreCase(doc, "null")) {
              String issue = "please document the meaning of null for field " + field.name() + '@'
                      + schema.getFullName();
              String source = schema.getProp("sourceIdl");
              if (source != null) {
                issue += " from " + source;
              }
              Schema.Type collectionType = getCollectionType(fs);
              if (collectionType != null) {
                issue += " and explain how its meaning is different from empty " + collectionType;
              }
              issues.add(issue);
            }
          }

        }
      }
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    public SchemaVisitorAction afterVisitNonTerminal(final Schema nonTerminal) {
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    @Nonnull
    public Result get() {
      return issues.isEmpty() ? Result.valid()
              : Result.failed("Schema " + root.getFullName() + " doc issues:\n" + String.join("\n", issues));
    }
  }

}
