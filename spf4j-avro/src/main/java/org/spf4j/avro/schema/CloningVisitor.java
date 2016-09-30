
package org.spf4j.avro.schema;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import org.apache.avro.Schema;
import static org.apache.avro.Schema.Type.RECORD;

/**
 * this visitor will create a clone of the original Schema with docs and other nonesential fields stripped
 * by default. what attributes are copied is customizable.
 * @author zoly
 */
public final class CloningVisitor implements SchemaVisitor<Schema> {

  private final IdentityHashMap<Schema, Schema> replace = new IdentityHashMap<>();

  private Schema root = null;

  private final BiConsumer<Schema, Schema> copyProperties;


  public CloningVisitor() {
    this(((BiConsumer<Schema, Schema>) Schemas::copyLogicalTypes).andThen(Schemas::copyAliases));
  }

  public CloningVisitor(final BiConsumer<Schema, Schema> copyProperties) {
    this.copyProperties = copyProperties;
  }

  @Override
  public SchemaVisitorAction visitTerminal(final Schema terminal) {
    if (root == null) {
      root = terminal;
    }
    Schema.Type type = terminal.getType();
    Schema newSchema;
    switch (type) {
      case RECORD: // recursion.
        if (!replace.containsKey(terminal)) {
          throw new IllegalStateException("Schema " + terminal + " must be already processed");
        }
        return SchemaVisitorAction.CONTINUE;
      case ARRAY:
      case MAP:
      case UNION:
        throw new IllegalStateException("Schema " + terminal + " should never be a terminal");
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
        newSchema = Schema.createEnum(terminal.getName(), null, terminal.getNamespace(), terminal.getEnumSymbols());
        break;
      case FIXED:
        newSchema = Schema.createFixed(terminal.getName(), null, terminal.getNamespace(), terminal.getFixedSize());
        break;
      default:
        throw new IllegalStateException("Unsupported schema " + terminal);
    }
    copyProperties.accept(terminal, newSchema);
    replace.put(terminal, newSchema);
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction visitNonTerminal(final Schema nt) {
    if (root == null) {
      root = nt;
    }
    Schema.Type type = nt.getType();
    if  (type == RECORD) {
        Schema newSchema = Schema.createRecord(nt.getName(), null, nt.getNamespace(), nt.isError());
        Schemas.copyAliases(nt, newSchema);
        copyProperties.accept(nt, newSchema);
        replace.put(nt, newSchema);
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction afterVisitNonTerminal(final Schema nt) {
     Schema.Type type = nt.getType();
     Schema newSchema;
     switch (type) {
       case RECORD:
         newSchema = replace.get(nt);
         List<Schema.Field> fields = nt.getFields();
         List<Schema.Field> newFields = new ArrayList<>(fields.size());
         for (Schema.Field field : fields) {
          Schema.Field newField = new Schema.Field(field.name(), replace.get(field.schema()),
                  null, field.defaultVal(), field.order());
          Schemas.copyAliases(field, newField);
          newFields.add(newField);
         }
         newSchema.setFields(newFields);
         return SchemaVisitorAction.CONTINUE;
       case UNION:
          List<Schema> types = nt.getTypes();
          List<Schema> newTypes = new ArrayList<>(types.size());
          for (Schema sch : types) {
            newTypes.add(replace.get(sch));
          }
          newSchema = Schema.createUnion(newTypes);

          break;
       case ARRAY:
         newSchema = Schema.createArray(replace.get(nt.getElementType()));
         break;
       case MAP:
         newSchema = Schema.createMap(replace.get(nt.getValueType()));
         break;
       default:
         throw new IllegalStateException("Illegal type " + type + ", schema " + nt);
     }
     copyProperties.accept(nt, newSchema);
     replace.put(nt, newSchema);
     return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public Schema get() {
    return replace.get(root);
  }

  @Override
  public String toString() {
    return "TrimNoneEsentialProperties{" + "replace=" + replace + ", root=" + root + '}';
  }

}
