package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Node implements Serializable, Comparable
{
  public final BigInteger p, q; ///< the candidate factors
  public final BigInteger product; ///< the partial factors for this node
  public final int        depth;
  private      double     h; ///< the heuristic search factors for this node

  Node() { this(null, 1, 1); }
  Node(final Node parent, int... bits)
  {
    this.depth = null != parent ? parent.depth+1 : 0;

    final BigInteger f1 = null != parent ? (1 == bits[0] ? parent.p.setBit(depth) : parent.p) : BigInteger.valueOf(bits[0]);
    final BigInteger f2 = null != parent ? (1 == bits[1] ? parent.q.setBit(depth) : parent.q) : BigInteger.valueOf(bits[1]);
    boolean larger = f1.compareTo(f2) < 0;

    this.p = larger ? f1 : f2;
    this.q = larger ? f2 : f1;
    this.product = p.multiply(q);
  }

  @Override public String toString() { return product.toString() + ":" + product.toString(Solver.cacheInternalBase) + ":" + depth + ":" + h + ":" + p.toString() + ":" + q.toString(); }
  @Override public boolean equals(Object o) { return o instanceof Node && ((Node) o).depth == depth && p.equals(((Node) o).p) && q.equals(((Node) o).q); }
  @Override public int compareTo(Object o) { return Double.compare(h(), ((Node) o).h()); }

  ///todo try 'magic' 193 and other values >> 2^n
  @Override public int hashCode()
  {
    int hash = 37 * depth + product.hashCode();
    hash = (37 * hash + p.hashCode());
    hash = (37 * hash + q.hashCode());
    return hash;
  }

  public String product() { return product.toString(Solver.internalBase()); }
  public String product(int base) { return product.toString(base); }

  /**
   * This function ensures that the current partial product resembles the target semiprime
   * in the currently fixed digit positions.
   * @return true if everything looks okay
   */
  boolean validFactors()
  {
    return product.testBit(depth) == Solver.cacheSemiprime.testBit(depth) &&
        (0 == Solver.cachePrimeLen1 || p.bitLength() <= Solver.cachePrimeLen1) &&
        (0 == Solver.cachePrimeLen2 || q.bitLength() <= Solver.cachePrimeLen2) &&
        product.bitLength() <= Solver.cacheSemiprimeBitLen;
  }

  /**
   * Ensure that none of the factors is trivial.
   * @return true if the factors look okay
   */
  boolean goal() { return Solver.cacheSemiprime.equals(product) && !BigInteger.ONE.equals(p) && !BigInteger.ONE.equals(q); }


  /**
   * Sums all desired heuristic functions.
   * @return an estimate of this node's distance to goal, where 0 = goal
   */
  private double h()
  {
    if (0 != h) return h;
    for (Heuristic heuristic : Solver.cacheHeuristics) h += heuristic.apply(this);
    return h;
  }

}
