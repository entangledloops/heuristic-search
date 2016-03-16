package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Node implements Serializable, Comparable
{
  private final AtomicBoolean locked = new AtomicBoolean(false); ///< is this node locked by a worker client?
  private final String key; ///< cached key value for performance

  final String[]     p;
  final BigInteger[] values;
  final BigInteger   product; ///< the partial factors for this node
  final double       h; ///< the heuristic search values for this node

  Node(final String[] p) { this(hash(p), p); }
  Node(final String key, final String... p)
  {
    if (null == p || 0 == p.length) throw new NullPointerException("bad prime or base");
    this.p = p;
    this.values = new BigInteger[p.length];

    final int internalBase = Solver.internalBase();
    for (int i = 0; i < p.length; ++i)
    {
      String formatted = p[i];
      if (Solver.safetyConscious())
      {
        while (formatted.startsWith("0") && formatted.length() > 0) formatted = formatted.substring(1);
        if ("".equals(formatted)) throw new NullPointerException("an impossibly \"even prime\" (" + i + ") was somehow generated");
      }
      values[i] = new BigInteger(formatted, internalBase);
    }

    this.product = Stream.of(values).reduce(BigInteger::multiply).get();
    this.h = h(p);
    this.key = null != key ? key : hash(p);
  }

  @Override public String toString() { return key; }
  @Override public boolean equals(Object o) { return o instanceof Node && ((Node) o).key.equals(key); }
  @Override public int compareTo(Object o) { return Double.compare(h, Node.class.cast(o).h); }

  public boolean validFactors()
  {
    for (BigInteger value : values) if (BigInteger.ONE.equals(value)) return false;
    return true;
  }

  public String p(int i, int base) { return base != Solver.internalBase() ? values[i].toString(base) : p[i]; }
  static String hash(String... p) { return Stream.of(p).reduce("", (p1,p2) -> p1 + ":" + p2); }

  /**
   * Calculate the heuristic value from the input strings of partial factors.
   * @param p array of strings representing candidate primes
   * @return an estimate of this node's distance to goal, where 0 = goal
   */
  private static double h(String... p)
  {
    if (null == p || 0 == p.length) return Double.POSITIVE_INFINITY;

    int[] p0s = new int[p.length];
    for (int i = 0; i < p0s.length; ++i)
    {
      if (null == p[i]) continue;
      for (final char c : p[i].toCharArray()) if ('0' == c) ++p0s[i];
    }

    final double p0sTo1s = ((double)(IntStream.of(p0s).sum()) / (double)(Stream.of(p).mapToInt(String::length).sum()));
    return Math.abs(p0sTo1s - Solver.semiprimeBinary0sTo1s);
  }
}
