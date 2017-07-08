
package org.spf4j.avro.schema;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import static org.apache.avro.Schema.Type.RECORD;
import org.apache.avro.ImmutableSchema;

/**
 * this visitor will create a clone of the original Schema with docs and other nonesential fields stripped
 * by default. what attributes are copied is customizable.
 * @author zoly
 */
public final class ImmutableCloningVisitor implements SchemaVisitor<ImmutableSchema> {

  private final IdentityHashMap<Schema, ImmutableSchema> replace = new IdentityHashMap<>();

  private final Schema root;

  private final BiConsumer<Field, Field> copyField;
  private final BiConsumer<Schema, Schema> copySchema;
  private final boolean copyDocs;

  public ImmutableCloningVisitor(final Schema root, final boolean serializationSignificatOnly) {
    this(serializationSignificatOnly ? SchemaUtils.FIELD_ESENTIALS : SchemaUtils.FIELD_EVERYTHING,
            serializationSignificatOnly ? SchemaUtils.SCHEMA_ESENTIALS : SchemaUtils.SCHEMA_EVERYTHING,
            !serializationSignificatOnly, root);
  }


  public ImmutableCloningVisitor(final BiConsumer<Field, Field> copyField,
          final BiConsumer<Schema, Schema> copySchema,
          final boolean copyDocs, final Schema root) {
    this.copyField = copyField;
    this.copySchema = copySchema;
    this.copyDocs = copyDocs;
    this.root = root;
  }

  @Override
  public SchemaVisitorAction visitTerminal(final Schema terminal) {
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
        newSchema = Schema.createEnum(terminal.getName(), copyDocs ? terminal.getDoc() : null,
                terminal.getNamespace(), terminal.getEnumSymbols());
        break;
      case FIXED:
        newSchema = Schema.createFixed(terminal.getName(), copyDocs ? terminal.getDoc() : null,
                terminal.getNamespace(), terminal.getFixedSize());
        break;
      default:
        throw new IllegalStateException("Unsupported schema " + terminal);
    }
    copySchema.accept(terminal, newSchema);
    replace.put(terminal, ImmutableSchema.create(newSchema));
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction visitNonTerminal(final Schema nt) {
    Schema.Type type = nt.getType();
    if  (type == RECORD) {
        Schema newSchema = Schema.createRecord(nt.getName(), copyDocs ? nt.getDoc() : null,
                nt.getNamespace(), nt.isError());
        copySchema.accept(nt, newSchema);
        replace.put(nt, ImmutableSchema.create(newSchema));
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
                  copyDocs ? field.doc() : null, field.defaultVal(), field.order());
          copyField.accept(field, newField);
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
     copySchema.accept(nt, newSchema);
     replace.put(nt, ImmutableSchema.create(newSchema));
     return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public ImmutableSchema get() {
    return replace.get(root);
  }

  @Override
  public String toString() {
    return "UnmodifiableCloningVisitor{" + "replace=" + replace + ", root=" + root + '}';
  }

}
