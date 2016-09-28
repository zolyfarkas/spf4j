package org.spf4j.avro.schema;

import com.google.common.annotations.Beta;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.spf4j.ds.IdentityHashSet;

/**
 * Avro Schema utilities, to traverse...
 * @author zoly
 */
@Beta
public final class Schemas {

  private Schemas() {
  }

  public static void copyAliases(final Schema from, final Schema to) {
    Set<String> aliases = from.getAliases();
    for (String alias : aliases) {
      to.addAlias(alias);
    }
  }

  public static void copyAliases(final Schema.Field from, final Schema.Field to) {
    Set<String> aliases = from.aliases();
    for (String alias : aliases) {
      to.addAlias(alias);
    }
  }

  public static void copyLogicalTypes(final Schema from, final Schema to) {
    LogicalType logicalType = from.getLogicalType();
    if (logicalType != null) {
      logicalType.addToSchema(to);
    }
  }

  public static void copyProperties(final Schema from, final Schema to) {
    Map<String, Object> objectProps = from.getObjectProps();
    for (Map.Entry<String, Object> entry : objectProps.entrySet()) {
      to.addProp(entry.getKey(), entry.getValue());
    }
  }

  public static boolean hasGeneratedJavaClass(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case ENUM:
      case RECORD:
      case FIXED:
        return true;
      default:
        return false;
    }
  }

  public static String getJavaClassName(final Schema schema) {
    String namespace = schema.getNamespace();
    if (namespace == null || namespace.isEmpty()) {
      return SpecificCompiler.mangle(schema.getName());
    } else {
      return namespace + '.' + SpecificCompiler.mangle(schema.getName());
    }
  }

  /**
   * depth first visit.
   *
   * @param start
   * @param visitor
   */
  public static <T> T visit(final Schema start, final SchemaVisitor<T> visitor) {
    // Set of Visited Schemas
    IdentityHashSet<Schema> visited = new IdentityHashSet<>();
    // Stack that contains the Schams to process and afterVisitNonTerminal functions.
    // Deque<Either<Schema, Supplier<SchemaVisitorAction>>>
    // Using either has a cost which we want to avoid...
    Deque<Object> dq = new ArrayDeque<>();
    dq.addLast(start);
    Object current;
    while ((current = dq.pollLast()) != null) {
      if (current instanceof Supplier) {
        // we are executing a non terminal post visit.
        SchemaVisitorAction action = ((Supplier<SchemaVisitorAction>) current).get();
        switch (action) {
          case CONTINUE:
            break;
          case SKIP_SUBTREE:
            throw new UnsupportedOperationException();
          case SKIP_SIBLINGS:
            //CHECKSTYLE:OFF InnerAssignment
            while ((current = dq.getLast()) instanceof Schema) {
              // just skip
            }
            //CHECKSTYLE:ON
            dq.addLast(current);
            break;
          case TERMINATE:
            return visitor.get();
          default:
            throw new UnsupportedOperationException("Invalid action " + action);
        }
      } else {
        Schema schema = (Schema) current;
        boolean terminate;
        if (!visited.contains(schema)) {
          Schema.Type type = schema.getType();
          switch (type) {
            case ARRAY:
              terminate = visitNonTerminal(visitor, schema, dq,
                      () -> Iterators.forArray(schema.getElementType()));
              visited.add(schema);
              break;
            case RECORD:
              terminate = visitNonTerminal(visitor, schema, dq,
                      () -> Iterators.transform(Lists.reverse(schema.getFields()).iterator(), (Field f) -> f.schema()));
              visited.add(schema);
              break;
            case UNION:
              terminate = visitNonTerminal(visitor, schema, dq, () -> schema.getTypes().iterator());
              visited.add(schema);
              break;
            case MAP:
              terminate = visitNonTerminal(visitor, schema, dq, () -> Iterators.forArray(schema.getValueType()));
              visited.add(schema);
              break;
            case NULL:
            case BOOLEAN:
            case BYTES:
            case DOUBLE:
            case ENUM:
            case FIXED:
            case FLOAT:
            case INT:
            case LONG:
            case STRING:
              terminate = visitTerminal(visitor, schema, dq);
              break;
            default:
              throw new UnsupportedOperationException("Invalid type " + type);
          }

        } else {
          terminate = visitTerminal(visitor, schema, dq);
        }
        if (terminate) {
            return visitor.get();
        }
      }
    }
    return visitor.get();
  }

  private static boolean visitNonTerminal(final SchemaVisitor visitor,
          final Schema schema, final Deque<Object> dq,
          final Supplier<Iterator<Schema>> itSupp) {
    SchemaVisitorAction action = visitor.visitNonTerminal(schema);
    switch (action) {
      case CONTINUE:
        dq.addLast((Supplier<SchemaVisitorAction>) () -> visitor.afterVisitNonTerminal(schema));
        Iterator<Schema> it = itSupp.get();
        while (it.hasNext()) {
          Schema child = it.next();
          dq.addLast(child);
        }
        break;
      case SKIP_SUBTREE:
        dq.addLast((Supplier<SchemaVisitorAction>) () -> visitor.afterVisitNonTerminal(schema));
        break;
      case SKIP_SIBLINGS:
        Object current;
        //CHECKSTYLE:OFF InnerAssignment
        while ((current = dq.getLast()) instanceof Schema) {
          // just skip
        }
        //CHECKSTYLE:ON
        dq.addLast(current);
        break;
      case TERMINATE:
        return true;
      default:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
    }
    return false;
  }

  private static boolean visitTerminal(final SchemaVisitor visitor, final Schema schema,
          final Deque<Object> dq) {
    SchemaVisitorAction action = visitor.visitTerminal(schema);
    switch (action) {
      case CONTINUE:
        break;
      case SKIP_SUBTREE:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
      case SKIP_SIBLINGS:
        Object current;
        //CHECKSTYLE:OFF InnerAssignment
        while ((current = dq.getLast()) instanceof Schema) {
          // just skip
        }
        //CHECKSTYLE:ON
        dq.addLast(current);
        break;
      case TERMINATE:
        return true;
      default:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
    }
    return false;
  }

}
