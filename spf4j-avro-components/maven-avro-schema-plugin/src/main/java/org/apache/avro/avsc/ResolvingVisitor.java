
package org.apache.avro.avsc;

import com.google.common.base.Function;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.compiler.schema.SchemaVisitor;
import org.apache.avro.compiler.schema.SchemaVisitorAction;
import org.apache.avro.compiler.schema.Schemas;

/**
 * this visitor will create a clone of the original Schema and will also resolve all unresolved schemas
 *
 * by default. what attributes are copied is customizable.
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class ResolvingVisitor implements SchemaVisitor<Schema> {

  private final IdentityHashMap<Schema, Schema> replace;
  private final Function<String, Schema> symbolTable;
  private final boolean  allowUndefinedLogicalTypes;
  private final Schema root;


  public ResolvingVisitor(final Schema root, final IdentityHashMap<Schema, Schema> replace,
          final Function<String, Schema> symbolTable, final boolean  allowUndefinedLogicalTypes) {
    this.replace = replace;
    this.symbolTable = symbolTable;
    this.root = root;
    this.allowUndefinedLogicalTypes = allowUndefinedLogicalTypes;
  }

  @Override
  public SchemaVisitorAction visitTerminal(final Schema terminal) {
    if (replace.containsKey(terminal)) {
      return SchemaVisitorAction.CONTINUE;
    }
    Schema.Type type = terminal.getType();
    Schema newSchema;
    switch (type) {
      case RECORD: // recursion.
      case ARRAY:
      case MAP:
      case UNION:
        if (!replace.containsKey(terminal)) {
          throw new IllegalStateException("Schema " + terminal + " must be already processed");
        }
        return SchemaVisitorAction.CONTINUE;
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        newSchema = Schema.create(type);
      break;
      case ENUM:
        newSchema = Schema.createEnum(terminal.getName(), terminal.getDoc(),
                terminal.getNamespace(), terminal.getEnumSymbols(), terminal.getEnumDefault());
        break;
      case FIXED:
        newSchema = Schema.createFixed(terminal.getName(), terminal.getDoc(),
                terminal.getNamespace(), terminal.getFixedSize());
        break;
      default:
        throw new IllegalStateException("Unsupported schema " + terminal);
    }
    copyAllProperties(terminal, newSchema);
    materializeLogicalType(newSchema);
    replace.put(terminal, newSchema);
    return SchemaVisitorAction.CONTINUE;
  }

  private void materializeLogicalType(final Schema schema) {
    schema.parseLogicalType(allowUndefinedLogicalTypes);
  }

  public static void copyAllProperties(final Schema first, final Schema second) {
    Schemas.copyLogicalTypes(first, second);
    Schemas.copyAliases(first, second);
    Schemas.copyProperties(first, second);
  }

  public static void copyAllProperties(final Field first, final Field second) {
    Schemas.copyAliases(first, second);
    Schemas.copyProperties(first, second);
  }

  @Override
  @SuppressFBWarnings({"LEST_LOST_EXCEPTION_STACK_TRACE", "EXS_EXCEPTION_SOFTENING_NO_CHECKED"})
  public SchemaVisitorAction visitNonTerminal(final Schema nt) {
    Schema.Type type = nt.getType();
    if  (type == Schema.Type.RECORD) {
        if (replace.containsKey(nt)) {
          return SchemaVisitorAction.SKIP_SUBTREE;
        }
        if (SchemaResolver.isUnresolvedSchema(nt)) {
          // unresolved schema will get a replacement that we already encountered,
          // or we will attempt to resolve.
          final String unresolvedSchemaName = SchemaResolver.getUnresolvedSchemaName(nt);
          Schema resSchema = symbolTable.apply(unresolvedSchemaName);
          if (resSchema == null && unresolvedSchemaName.indexOf('.') < 0) { // try parent namespace
            resSchema = symbolTable.apply(root.getNamespace() + '.' + unresolvedSchemaName);
          }
          if (resSchema == null) {
            throw new AvroTypeException("Unable to resolve " + unresolvedSchemaName);
          }
          Schema replacement = replace.get(resSchema);
          if (replacement == null) {
            try {
            replace.put(nt, Schemas.visit(resSchema, new ResolvingVisitor(resSchema,
                    replace, symbolTable, allowUndefinedLogicalTypes)));
            } catch (StackOverflowError err) {
              throw new IllegalStateException("Stack overflow while resolving " + resSchema.getName());
            }
          } else {
            replace.put(nt, replacement);
          }
        } else {
          // create a fieldless clone. Fields will be added in afterVisitNonTerminal.
          Schema newSchema = Schema.createRecord(nt.getName(), nt.getDoc(), nt.getNamespace(), nt.isError());
          copyAllProperties(nt, newSchema);
          replace.put(nt, newSchema);
        }
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction afterVisitNonTerminal(final Schema nt) {
     Schema.Type type = nt.getType();
     Schema newSchema;
     switch (type) {
       case RECORD:
         if (!SchemaResolver.isUnresolvedSchema(nt)) {
            newSchema = replace.get(nt);
            List<Schema.Field> fields = nt.getFields();
            List<Schema.Field> newFields = new ArrayList<Schema.Field>(fields.size());
            for (Schema.Field field : fields) {
              Schema fieldSchema = field.schema();
              Schema get = replace.get(fieldSchema);
              if (get == null) {
                throw new RuntimeException("No replacement for " + fieldSchema);
              }
             Schema.Field newField = new Schema.Field(field.name(), get,
                     field.doc(), field.defaultVal(), field.order());
             copyAllProperties(field, newField);
             newFields.add(newField);
            }
            newSchema.setFields(newFields);
            materializeLogicalType(newSchema);
         }
         return SchemaVisitorAction.CONTINUE;
       case UNION:
          if (replace.containsKey(nt)) {
            return SchemaVisitorAction.CONTINUE;
          }
          List<Schema> types = nt.getTypes();
          List<Schema> newTypes = new ArrayList<Schema>(types.size());
          for (Schema sch : types) {
            newTypes.add(replace.get(sch));
          }
          newSchema = Schema.createUnion(newTypes);
          break;
       case ARRAY:
        if (replace.containsKey(nt)) {
            return SchemaVisitorAction.CONTINUE;
         }
         newSchema = Schema.createArray(replace.get(nt.getElementType()));
         break;
       case MAP:
         if (replace.containsKey(nt)) {
            return SchemaVisitorAction.CONTINUE;
          }
         newSchema = Schema.createMap(replace.get(nt.getValueType()));
         break;
       default:
         throw new IllegalStateException("Illegal type " + type + ", schema " + nt);
     }
     copyAllProperties(nt, newSchema);
     materializeLogicalType(newSchema);
     replace.put(nt, newSchema);

     return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public Schema get() {
    return replace.get(root);
  }

  @Override
  public String toString() {
    return "ResolvingVisitor{" + "replace=" + replace + ", symbolTable=" + symbolTable + ", root=" + root + '}';
  }

}
