package org.apache.avro;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
public interface SchemaResolver {

  @Nonnull
  Schema resolveSchema(String id);

  @Nullable
  String getId(Schema schema);

  SchemaResolver NONE = new SchemaResolver() {
    @Override
    public Schema resolveSchema(final String id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getId(final Schema schema) {
      return null;
    }
  };

  default void registerAsDefault() {
    SchemaResolvers.registerDefault(this);
  }

}
