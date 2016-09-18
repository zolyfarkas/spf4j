package org.spf4j.avro.schema;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Supplier;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.spf4j.base.Either;
import org.spf4j.ds.IdentityHashSet;

/**
 *
 * @author zoly
 */
public final class Schemas {

  private Schemas() {
  }

  /**
   * depth first visit.
   *
   * @param start
   * @param visitor
   */
  public static void visit(final Schema start, final SchemaVisitor visitor) {
    // Set of Visited Schemas
    IdentityHashSet<Schema> visited = new IdentityHashSet<>();
    // Stack that contains the Schams to process and afterVisitNonTerminal functions.
    Deque<Either<Schema, Supplier<SchemaVisitorAction>>> dq = new ArrayDeque<>();
    dq.addLast(Either.left(start));
    Either<Schema, Supplier<SchemaVisitorAction>> current;
    while ((current = dq.pollLast()) != null) {
      if (current.isRight()) {
        // we are executing a non terminal post visit.
        SchemaVisitorAction action = current.getRight().get();
        switch (action) {
          case CONTINUE:
            break;
          case SKIP_SUBTREE:
            throw new UnsupportedOperationException();
          case SKIP_SIBLINGS:
            while ((current = dq.getLast()).isLeft()) {
              // just skip
            }
            dq.addLast(current);
            break;
          case TERMINATE:
            return;
          default:
            throw new UnsupportedOperationException("Invalid action " + action);
        }
      } else {
        Schema schema = current.getLeft();
        if (!visited.contains(schema)) {
          Schema.Type type = schema.getType();
          boolean terminate;
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
              terminate = visitNonTerminal(visitor, schema, dq, () -> Iterators.forArray(schema.getElementType()));
              visited.add(schema);
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
          if (terminate) {
            return;
          }
        }
      }
    }
  }

  private static boolean visitNonTerminal(final SchemaVisitor visitor,
          final Schema schema, final Deque<Either<Schema, Supplier<SchemaVisitorAction>>> dq,
          final Supplier<Iterator<Schema>> itSupp) {
    SchemaVisitorAction action = visitor.visitNonTerminal(schema);
    switch (action) {
      case CONTINUE:
        dq.addLast(Either.right(() -> visitor.afterVisitNonTerminal(schema)));
        Iterator<Schema> it = itSupp.get();
        while (it.hasNext()) {
          Schema child = it.next();
          dq.addLast(Either.left(child));
        }
        break;
      case SKIP_SUBTREE:
        dq.addLast(Either.right(() -> visitor.afterVisitNonTerminal(schema)));
        break;
      case SKIP_SIBLINGS:
        Either<Schema, Supplier<SchemaVisitorAction>> current;
        while ((current = dq.getLast()).isLeft()) {
          // just skip
        }
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
          final Deque<Either<Schema, Supplier<SchemaVisitorAction>>> dq) {
    SchemaVisitorAction action = visitor.visitTerminal(schema);
    switch (action) {
      case CONTINUE:
        break;
      case SKIP_SUBTREE:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
      case SKIP_SIBLINGS:
        Either<Schema, Supplier<SchemaVisitorAction>> current;
        while ((current = dq.getLast()).isLeft()) {
          // just skip
        }
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
