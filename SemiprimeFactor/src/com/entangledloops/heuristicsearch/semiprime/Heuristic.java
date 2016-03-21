package com.entangledloops.heuristicsearch.semiprime;

import java.math.BigInteger;
import java.util.function.Function;

/**
 * @author Stephen Dunn
 * @since March 20, 2016
 */
public enum Heuristic
{
  /**
   * Calculate distribution difference from target.
   *
   * abs( sum(factor[i].bitCount() / factor[i].bitLength()) - (targetBitCount / targetBitLen) )
   */
  DIST_DIFF_BY_LEN("Distribution Difference by Length",
      (n) ->
      {
        double h = 0;
        for (BigInteger factor : n.factors()) h += ((double)factor.bitCount() / (double)factor.bitLength());
        return Math.abs(h - Solver.semiprime1sToLen);
      }
  ),
  /**
   * Calculate distribution difference from target.
   *
   * abs( [ sum(factor[i].bitCount()) / (numFactors * (depth+1)) ] - (targetBitCount / targetBitLen) )
   */
  DIST_DIFF_BY_DEPTH("Distribution Difference by Depth",
      (n) ->
      {
        double h = 0;
        for (BigInteger factor : n.factors()) h += factor.bitCount();
        return Math.abs((h / (double)(n.factors().length * (1+n.depth()))) - Solver.semiprime1sToLen);
      }
  ),
  /**
   * Calculate h based upon the likelihood that the current factor bit distribution
   * reflects expectations based upon objective experimental results w/semiprime numbers.
   */
  DIST_EXPECTED("Expected Distribution",
      (n) ->
      {
        double h = 0;

        return h;
      }
  ),
  /**
   * Hamming distance to goal as.
   *
   * for each bit i in target:
   *   sum( n.product[i] != target[i] )
   */
  HAMMING("Hamming Distance",
      (n) ->
      {
        double h = 0;
        for (int i = 0; i < Solver.semiprimeBitLen; ++i) if (Solver.semiprime().testBit(i) != n.product.testBit(i)) ++h;
        return h;
      }
  ),
  ;

  private final String name;
  private final Function<Node, Double> function;
  Heuristic(String name, Function<Node, Double> function) { this.name = name; this.function = function; }

  @Override public String toString() { return name; }
  public Double apply(Node n) { return function.apply(n); }

  public static Heuristic byName(String name)
  {
    for (Heuristic h : Heuristic.values()) if (h.toString().equals(name)) return h;
    return null;
  }
}
