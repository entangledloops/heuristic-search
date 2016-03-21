package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;

/**
 * @author Stephen Dunn
 * @since November 4, 2015
 */
public class Packet implements Serializable
{
  public enum Type
  {
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

  public Type type() { return type; }

  // raw packet data

  public Object[] data() { return data; }
  public <T> T data(int i, Class<T> klass) { return klass.cast(data[i]); }

  // some packet-parsing helpers

  public int size() { return null != data ? data.length : 0; }
  public String string() { return data(0, String.class); }
}
