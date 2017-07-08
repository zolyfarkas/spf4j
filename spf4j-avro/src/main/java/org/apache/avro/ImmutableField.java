
package org.apache.avro;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.apache.avro.Schema.Field;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

/**
 * @author zoly
 */
@Beta
@Immutable
public final class ImmutableField extends Field {

  private final Field wrapped;

  private ImmutableField(final Field field) {
    super(field.name(), field.schema(), field.doc(), field.defaultVal(), field.order());
    this.wrapped = field;
  }

  public static ImmutableField create(final Field field) {
      return new ImmutableField(field);
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public Set<String> aliases() {
    return wrapped.aliases();
  }

  @Override
  public void addAlias(final String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int pos() {
    return wrapped.pos();
  }


  @Override
  void writeProps(final JsonGenerator gen) throws IOException {
    wrapped.writeProps(gen);
  }

  @Override
  public Map<String, Object> getObjectProps() {
    return wrapped.getObjectProps();
  }

  @Override
  public Map<String, JsonNode> getJsonProps() {
    return wrapped.getJsonProps();
  }

  @Override
  Map<String, JsonNode> jsonProps(final Map<String, String> stringProps) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> getProps() {
    return wrapped.getProps();
  }

  @Override
  public void addProp(final String name, final Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addProp(final String name, final JsonNode value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addProp(final String name, final String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getObjectProp(final String name) {
    return wrapped.getObjectProp(name);
  }

  @Override
  public JsonNode getJsonProp(final String name) {
    return wrapped.getJsonProp(name);
  }

  @Override
  public String getProp(final String name) {
    return wrapped.getProp(name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.wrapped);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ImmutableField other = (ImmutableField) obj;
    return Objects.equals(this.wrapped, other.wrapped);
  }

}
