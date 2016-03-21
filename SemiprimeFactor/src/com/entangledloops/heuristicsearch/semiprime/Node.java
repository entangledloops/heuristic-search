package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Node implements Serializable, Comparable
{
  private final int  depth;
  private final BigInteger[] factors; ///< the candidate factors
  final         BigInteger   product; ///< the partial factors for this node
  final         double       h; ///< the heuristic search factors for this node

  Node() { this(null, 1, 1); }
  Node(final Node parent, int... bits)
  {
    this.depth = null != parent ? parent.depth+1 : 0;
    this.factors = new BigInteger[bits.length]; for (int i = 0; i < bits.length; ++i) this.factors[i] = null != parent ? (1 == bits[i] ? parent.factors[i].setBit(depth) : BigInteger.ZERO.add(parent.factors[i])) : BigInteger.valueOf(bits[i]);
    this.product = Stream.of(this.factors).reduce(BigInteger::multiply).orElseThrow(RuntimeException::new);
    this.h = h();
  }

  @Override public String toString() { return product.toString() + ":" + product.toString(Solver.internalBase()) + ":" + depth + ":" + h + ":" + Stream.of(factors).sequential().skip(1).map(Object::toString).reduce(factors[0].toString(), (p1,p2) -> p1 + ":" + p2); }
  @Override public boolean equals(Object o) { return o instanceof Node && ((Node) o).depth == depth && ((Node) o).product.equals(product) && Stream.of(factors).allMatch(i -> Stream.of(((Node)o).factors).anyMatch(i::equals)); }
  @Override public int compareTo(Object o) { return Double.compare(h, ((Node) o).h); }

  ///todo try 'magic' 193 and other values >> 2^n
  @Override public int hashCode()
  {
    final AtomicInteger hash = new AtomicInteger(depth);
    hash.set(37 * hash.get() + product.hashCode());
    Stream.of(factors).sorted().forEach((i) -> hash.set(37 * hash.get() + i.hashCode()));
    return hash.get();
  }

  public int depth() { return depth; }
  public String product() { return product.toString(Solver.internalBase()); }
  public String product(int base) { return product.toString(base); }
  public BigInteger factor(int i) { return factors[i]; }
  public BigInteger[] factors() { return factors; }

  /**
   * This function ensures that the current partial product resembles the target semiprime
   * in the currently fixed digit positions.
   * @return true if everything looks okay
   */
  boolean validFactors()
  {
    return depth < Solver.length() &&
        (0 == Solver.prime1Len() || depth < Solver.prime1Len()) &&
        (0 == Solver.prime2Len() || depth < Solver.prime2Len()) &&
        product.bitLength() <= Solver.semiprimeBitLen &&
        product.testBit(depth) == Solver.semiprime().testBit(depth);
  }

  /**
   * Ensure that none of the factors is trivial.
   * @return true if the factors look okay
   */
  boolean goalFactors() { for (BigInteger value : factors) if (BigInteger.ONE.equals(value)) return false; return true; }


  /**
   * Sums all desired heuristic functions.
   * @return an estimate of this node's distance to goal, where 0 = goal
   */
  private double h()
  {
    // initialize h to 0 or offset by depth if factor lengths were known in advance ("g-weighting")
    double h = 0 != Solver.maxFactorLen ? Solver.maxFactorLen - (depth+1) : 0;

    // if h is negative, we must have exceeded the max factor length that known a priori, so set to inf
    if (h < 0) return Double.POSITIVE_INFINITY;

    // sum desired heuristics
    for (Heuristic heuristic : Solver.heuristics()) h += heuristic.apply(this);

    ///@todo separate g, h, f properly
    return h;
  }

}
