package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author Stephen Dunn
 * @since November 4, 2015
 */
public class Packet implements Serializable
{
  public enum Type
  {
    UPDATE,
    TARGET_UPDATE,
    USERNAME_UPDATE,
    EMAIL_UPDATE,
    OPEN_UPDATE,
    OPEN_CHECK,
    CLOSED_UPDATE,
    CLOSED_CHECK,
    SOLUTION_UPDATE,
    ERROR
  }

  private final Type type;
  private final Object[] data;

  public Packet(Type type, Object... data)
  {
    this.type = type;
    this.data = data;
  }

  @Override public String toString() { return type.name() + " : " + size(); }

  public Type type() { return type; }
  public int size() { return null != data ? data.length : 0; }

  public Object[] data() { return data; }
  public <T> T data(int i, Class<T> klass) { return klass.cast(data[i]); }

  public String asString() { return asString(0); }
  public String asString(int i) { return data(i, String.class); }

  public <T> Collection<T> asCollection() { return asCollection(0); }
  public <T> Collection<T> asCollection(int i) { return (Collection<T>) data[i]; }

  public <T> List<T> asList() { return asList(0); }
  public <T> List<T> asList(int i) { return (List<T>) data[i]; }
}
