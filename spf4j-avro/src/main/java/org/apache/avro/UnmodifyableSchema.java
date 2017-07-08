package org.apache.avro;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.codehaus.jackson.JsonNode;

/**
 * @author zoly
 */
//CHECKSTYLE IGNORE EqualsHashCode FOR NEXT 1000 LINES
public final class UnmodifyableSchema extends Schema {

  private final Schema wrapped;

  private UnmodifyableSchema(final Schema schema) {
    super(schema.getType());
    this.wrapped = schema;
  }

  public static UnmodifyableSchema create(final Schema schema) {
      return new UnmodifyableSchema(schema);
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
    final UnmodifyableSchema other = (UnmodifyableSchema) obj;
    return Objects.equals(this.wrapped, other.wrapped);
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public int getFixedSize() {
    return wrapped.getFixedSize();
  }

  @Override
  public Integer getIndexNamed(final String name) {
    return wrapped.getIndexNamed(name);
  }

  @Override
  public List<Schema> getTypes() {
    return wrapped.getTypes();
  }

  @Override
  public Schema getValueType() {
    return wrapped.getValueType();
  }

  @Override
  public Schema getElementType() {
    return wrapped.getElementType();
  }

  @Override
  public boolean isError() {
    return wrapped.isError();
  }

  @Override
  public Set<String> getAliases() {
    return wrapped.getAliases();
  }

  @Override
  public void addAlias(final String alias, final String space) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAlias(final String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFullName() {
    return wrapped.getFullName();
  }

  @Override
  public String getNamespace() {
    return wrapped.getNamespace();
  }

  @Override
  public String getDoc() {
    return wrapped.getDoc();
  }

  @Override
  public String getName() {
    return wrapped.getName();
  }

  @Override
  public boolean hasEnumSymbol(final String symbol) {
    return wrapped.hasEnumSymbol(symbol);
  }

  @Override
  public int getEnumOrdinal(final String symbol) {
    return wrapped.getEnumOrdinal(symbol);
  }

  @Override
  public List<String> getEnumSymbols() {
    return super.getEnumSymbols();
  }

  @Override
  public void setFields(final List<Field> fields) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Field> getFields() {
    return wrapped.getFields();
  }

  @Override
  public Field getField(final String fieldname) {
    return wrapped.getField(fieldname);
  }

  @Override
  public LogicalType getLogicalType() {
    return wrapped.getLogicalType();
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
  public Map<String, Object> getObjectProps() {
    return wrapped.getObjectProps();
  }

  @Override
  public Map<String, JsonNode> getJsonProps() {
    return wrapped.getJsonProps();
  }

  @Override
  public Map<String, String> getProps() {
    return wrapped.getProps();
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

}
