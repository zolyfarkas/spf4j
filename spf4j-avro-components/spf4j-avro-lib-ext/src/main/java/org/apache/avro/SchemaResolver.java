package org.apache.avro;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
public interface SchemaResolver {

  /**
   * Lower level resolver implementation, that will write a schema in a "custom way"
   * @param schema
   * @param gen
   * @return
   * @throws IOException
   */
  default boolean customWrite(Schema schema, JsonGenerator gen) throws IOException {
    String ref = getId(schema);
    if (ref != null) {
        gen.writeStartObject();
        gen.writeFieldName(getJsonAttrName());
        gen.writeString(ref);
        gen.writeEndObject();
        return true;
    } else {
      return false;
    }
  }

  /**
   * Lowere level resolver implementation that will read a schema from a "custom way"
   * @param object
   * @return
   */
  @Nullable
  default Schema customRead(Function<String, JsonNode> object) {
    JsonNode refVal = object.apply(getJsonAttrName());
    if (refVal != null) {
      return resolveSchema(refVal.asText());
    } else {
      return null;
    }
  }

  default String getJsonAttrName() {
    return "$ref";
  }

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
