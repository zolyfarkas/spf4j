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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.maven.plugin.MojoExecutionException;
import org.spf4j.avro.schema.SchemaVisitor;
import org.spf4j.avro.schema.SchemaVisitorAction;
import org.spf4j.maven.plugin.avro.avscp.validation.Validator;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.maven.plugin.avro.avscp.ValidatorMojo;
import org.spf4j.maven.plugin.avro.avscp.validation.Validators;

/**
 * Validates schema names to be compliant with: no underscores in names (camel case should bd used instead) names must
 * have a minimum size.
 *
 * <p>this validator ("namesValidator") has the following configurations:</p>
 * <ul>
 *  <li>minNameSize - minimum number of characters that a name must have </li>
 *  <li>camelCase - validate the use of camel case, will complain if names contain '_'</li>
 *  <li>validTypeNames - a comma separated list of allowed type names, defaults to ""</li>
 *  <li>invalidTypeNames - a comma separated list of type names that are not allowed, default to "" </li>
 *  <li>validFieldNames - a comma separated list of allowed field names, defaults to ""</li>
 *  <li>invalidFieldNames - a comma separated list of field names that are not allowed, default to "" </li>
 * </ul>
 * @author Zoltan Farkas
 */
public final class SchemaNamesValidator implements Validator<Schema> {

  @Override
  public String getName() {
    return "namesValidator";
  }

  @Override
  @Nonnull
  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // not in this case
  public Result validate(final Schema schema, final ValidatorMojo mojo) throws MojoExecutionException, IOException {
    Map<String, String> cfg = mojo.getValidatorConfigs();
    try {
      final Set<String> validTypeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      validTypeNames.addAll(Csv.readRow(cfg.getOrDefault("validTypeNames", "")));
      final Set<String> invalidTypeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      invalidTypeNames.addAll(Csv.readRow(cfg.getOrDefault("invalidTypeNames", "")));
      final Set<String> validFieldNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      validFieldNames.addAll(Csv.readRow(cfg.getOrDefault("validFieldNames", "x,y,z")));
      final Set<String> invalidFieldNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      invalidFieldNames.addAll(Csv.readRow(cfg.getOrDefault("invalidFieldNames", "")));
      return Schemas.visit(schema, new NamesValidatorVisitor(
              Integer.parseInt(cfg.getOrDefault("minNameSize", "3")), validTypeNames, invalidTypeNames,
              validFieldNames, invalidFieldNames,
              Boolean.parseBoolean(cfg.getOrDefault("camelCase", "true")), schema));
    } catch (CsvParseException ex) {
      throw new MojoExecutionException("Configuration issue with " + getName() + ", cfg = " + cfg, ex);
    }
  }

  @Override
  public Class<Schema> getValidationInput() {
    return Schema.class;
  }

  private static class NamesValidatorVisitor implements SchemaVisitor<Result> {

    private final boolean camelCase;

    private final int minNameSize;

    private final Set<String> validTypeNames;

    private final Set<String> invalidTypeNames;

    private final Set<String> validFieldNames;

    private final Set<String> invalidFieldNames;

    private final List<String> issues;

    private final Schema root;

    NamesValidatorVisitor(final int minNameSize,
            final Set<String> validTypeNames,
            final Set<String> invalidTypeNames,
            final Set<String> validFieldNames,
            final Set<String> invalidFieldNames,
            final boolean camelCase,
            final Schema root) {
      issues = new ArrayList<>(4);
      this.minNameSize = minNameSize;
      this.validTypeNames = validTypeNames;
      this.validFieldNames = validFieldNames;
      this.invalidTypeNames = invalidTypeNames;
      this.invalidFieldNames = invalidFieldNames;
      this.root = root;
      this.camelCase = camelCase;
    }

    private void validateTypeName(final String name) {
      if (validTypeNames.contains(name)) {
        return;
      }
      if (invalidTypeNames.contains(name)) {
        issues.add("Invalid type name: " + name);
      }
      if (name.length() < minNameSize) {
        issues.add("Invalid type name, too short: " + name);
      }
      if (camelCase && name.contains("_")) {
        issues.add("Invalid type name, use camel case: " + name);
      }
    }

    private void validateFieldName(final String name) {
      if (validFieldNames.contains(name)) {
        return;
      }
      if (invalidFieldNames.contains(name)) {
        issues.add("Invalid field name: " + name);
      }
      if (name.length() < minNameSize) {
        issues.add("Invalid field name, too short: " + name);
      }
      if (camelCase && name.contains("_")) {
        issues.add("Invalid field name, use camel case: " + name);
      }
    }

    @Override
    public SchemaVisitorAction visitTerminal(final Schema schema) {
      switch (schema.getType()) {
        case ENUM:
        case FIXED:
          validateTypeName(schema.getName());
          break;
        default:
      }
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    public SchemaVisitorAction visitNonTerminal(final Schema schema) {
      if (schema.getType() == Schema.Type.RECORD) {
        if (Validators.skipValidator(schema, "namesValidator")) {
          return SchemaVisitorAction.CONTINUE;
        }
        validateTypeName(schema.getName());
        for (Field field : schema.getFields()) {
          validateFieldName(field.name());
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
              : Result.failed("Schema " + root.getFullName() + " naming issues:\n" + String.join("\n", issues));
    }
  }

}
