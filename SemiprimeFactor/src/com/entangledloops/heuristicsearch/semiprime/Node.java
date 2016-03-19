package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Node implements Serializable, Comparable
{
  private final String       key; ///< cached key value for performance
  private final String[]     factors; ///< the candidate prime factors as strings
  private final BigInteger[] values; ///< the candidate factors
  final         BigInteger   product; ///< the partial factors for this node
  final         double       h; ///< the heuristic search values for this node

  Node(final String[] factors) { this(hash(factors), factors); }
  Node(final String key, final String... factors)
  {
    if (null == factors || 0 == factors.length) throw new NullPointerException("bad prime or base");
    this.factors = factors;
    this.values = new BigInteger[factors.length];

    for (int i = 0; i < factors.length; ++i)
    {
      int pos = 0; for (char c : factors[0].toCharArray()) { if ('0' == c) ++pos; else break; }
      final String formatted = factors[0].substring(pos);
      if (Solver.safetyConscious() && "".equals(formatted)) throw new NullPointerException("an empty factor candidate was generated");
      values[i] = new BigInteger(formatted, Solver.internalBase());
    }

    this.product = Stream.of(values).reduce(BigInteger::multiply).get();
    this.h = h(factors);
    this.key = null != key ? key : hash(factors);
  }

  @Override public String toString() { return key; }
  @Override public boolean equals(Object o) { return o instanceof Node && ((Node) o).key.equals(key); }
  @Override public int compareTo(Object o) { return Double.compare(h, Node.class.cast(o).h); }

  public String product() { return product.toString(Solver.internalBase()); }
  public String product(int base) { return product.toString(base); }

  /**
   * This function ensures that the current partial product resembles the target semiprime
   * in the currently fixed digit positions.
   * @return true if everything looks okay
   */

  boolean validFactors()
  {
    final int depth = factors[0].length();
    return product.testBit(depth) == Solver.semiprime().testBit(depth);
  }

  /**
   * Ensure that none of the factors is trivial.
   * @return true if the factors look okay
   */
  boolean goalFactors()
  {
    for (BigInteger value : values) if (BigInteger.ONE.equals(value)) return false;
    return true;
  }

  public String factor(int i) { return factors[i]; }
  public String factor(int i, int base) { return base != Solver.internalBase() ? values[i].toString(base) : factors[i]; }

  /**
   * Computes a unique lookup key for the node.
   * @param p
   * @return
   */
  static String hash(String... p) { return Stream.of(p).skip(1).reduce(p[0], (p1,p2) -> p1 + ":" + p2); }

  /**
   * This heuristic takes each prime's difference of binary 0s/(0s+1s) from the target and sums.
   * @param factors array of strings representing candidate primes
   * @return an estimate of this node's distance to goal, where 0 = goal
   */
  private static double h(String... factors)
  {
    if (null == factors || 0 == factors.length) return Double.POSITIVE_INFINITY;

    double h = Math.max(Solver.prime1Len(), Solver.prime2Len());
    for (String factor : factors)
    {
      int p0s = 0; for (final char c : factor.toCharArray()) { if ('0' == c) ++p0s; }
      h += Math.abs(((double) p0s / (double) factor.length()) - Solver.semiprimeBinary0sTo1s);
    }

    return h;
  }

  /**
   * This heuristic sums all prime candidate's 0s and divides the total by (0s+1s),
   * then returns the difference from the target.
   * @param p array of strings representing candidate primes
   * @return an estimate of this node's distance to goal, where 0 = goal
   */
  private static double h2(String... p)
  {
    if (null == p || 0 == p.length) return Double.POSITIVE_INFINITY;

    int[] p0s = new int[p.length];
    for (int i = 0; i < p0s.length; ++i) { for (final char c : p[i].toCharArray()) if ('0' == c) ++p0s[i]; }

    final double p0sTo1s = ((double)(IntStream.of(p0s).sum()) / (double)(Stream.of(p).mapToInt(String::length).sum()));
    return Math.abs(p0sTo1s - Solver.semiprimeBinary0sTo1s);
  }
}
