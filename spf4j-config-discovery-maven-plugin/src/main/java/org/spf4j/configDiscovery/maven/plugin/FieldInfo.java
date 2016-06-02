package org.spf4j.configDiscovery.maven.plugin;

import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class FieldInfo<T> {

  private final Class<T> type;

  private final Object defaultValue;

  private final String doc;

  public FieldInfo(final String doc, final Class<T> type, final T defaultValue) {
    this.type = type;
    this.defaultValue = defaultValue;
    this.doc = doc;
  }

  public Class<?> getType() {
    return type;
  }

  @Nullable
  public Object getDefaultValue() {
    return defaultValue;
  }

  public String getDoc() {
    return doc;
  }

  @Override
  public String toString() {
    return "FieldInfo{" + "type=" + type + ", defaultValue=" + defaultValue + ", doc=" + doc + '}';
  }



}
