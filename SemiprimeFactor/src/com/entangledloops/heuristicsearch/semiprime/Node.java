package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Node implements Serializable, Comparable
{
  private final boolean   identicalFactors;
  private final int       hashCode;
  final        int        depth;
  public final BigInteger p, q; ///< the candidate factors
  public final BigInteger product; ///< the partial factors for this node
  private      double     h; ///< the heuristic search factors for this node

  int hashcount=0;
  Node() { this(null, 1, 1); }
  Node(final Node parent, int pBit, int qBit)
  {
    this.depth = null != parent ? parent.depth+1 : 0;

    final BigInteger f1 = null != parent ? (0 != pBit ? parent.p.setBit(depth) : parent.p) : BigInteger.valueOf(pBit);
    final BigInteger f2 = null != parent ? (0 != qBit ? parent.q.setBit(depth) : parent.q) : BigInteger.valueOf(qBit);

    final int compare = f1.compareTo(f2);
    this.identicalFactors = compare == 0;
    this.p = compare < 0 ? f1 : f2;
    this.q = compare < 0 ? f2 : f1;

    this.product = p.multiply(q);

    // cache the hash for performance during table lookups
    int hash = 37 * depth + product.hashCode();
    hash = 37 * hash + p.hashCode();
    hash = 37 * hash + q.hashCode();
    hashCode = hash;
  }

  @Override public String toString() { return product + "<sub>10</sub>:" + product.toString(Solver.cacheInternalBase) + "<sub>" + Solver.cacheInternalBase + "</sub>:" + depth + ":depth:" + p + ":p:" + q + ":q:" + h  + ":h:" + hashCode + ":hash"; }
  @Override public boolean equals(Object o) { return o instanceof Node && ((Node) o).depth == depth && p.equals(((Node) o).p) && q.equals(((Node) o).q); }
  @Override public int compareTo(Object o) { return Double.compare(h(), ((Node) o).h()); }
  @Override public int hashCode() { if (++hashcount > 1) Log.o(hashcount + " : " + toString()); return hashCode; }

  boolean identicalFactors() { return identicalFactors; }

  /**
   * This function ensures that the current partial product resembles the target semiprime
   * in the currently fixed digit positions.
   * @return true if everything looks okay
   */
  boolean validFactors()
  {
    return
        product.testBit(depth) == Solver.cacheSemiprime.testBit(depth) &&
        product.bitLength() <= Solver.cacheSemiprimeBitLen;
  }

  /**
   * Ensure that none of the factors is trivial.
   * @return true if this node is the goal
   */
  boolean goal()
  {
    return
        (0 == Solver.cachePLength || (1+depth) == Solver.cachePLength) &&
        (0 == Solver.cacheQLength || (1+depth) == Solver.cacheQLength) &&
        Solver.cacheSemiprime.equals(product) &&
        !BigInteger.ONE.equals(p) &&
        !BigInteger.ONE.equals(q);
  }


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
