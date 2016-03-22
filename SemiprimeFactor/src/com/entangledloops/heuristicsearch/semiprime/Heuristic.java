package com.entangledloops.heuristicsearch.semiprime;

import java.util.function.Function;

/**
 * @author Stephen Dunn
 * @since March 20, 2016
 */
public enum Heuristic
{
  DIST_EXPECTED_SEPARATE("Expected Distribution (separate)",
      "Calculate distribution difference from target.\nabs( sum(factor[i].bitCount() / factor[i].bitLength()) - (targetBitCount / targetBitLen) )",
      (n) -> Math.abs((((double)n.p.bitCount() / (double)n.p.bitLength()) + ((double)n.q.bitCount() / (double)n.q.bitLength())) - Solver.cacheSemiprimeBitCountOverBitLen)
  ),

  DIST_EXPECTED_SUMMED("Expected Distribution (summed)",
      "Calculate h based upon the likelihood that the current factor bit distribution reflects\nexpectations based upon objective experimental results w/semiprime numbers.",
      (n) -> Math.abs(((double)(n.p.bitCount() + n.q.bitCount())/((double)(n.p.bitLength() + n.q.bitLength()))) - Solver.cacheSemiprimeBitCountOverBitLen)
  ),

  DIST_DIFF_BY_DEPTH_SEPARATE("Distribution Difference by Depth (separate)",
      "Calculate distribution difference from target.\nabs( [ ((p.bitCount/(1+depth))+(q.bitCount/(1+depth)) ] - (targetBitCount/targetBitLen) )",
      (n) -> Math.abs( (((double)n.p.bitCount()/(1.0+n.depth)) + ((double)n.q.bitCount()/(1.0+n.depth))) - Solver.cacheSemiprimeBitCountOverBitLen)
  ),

  DIST_DIFF_BY_DEPTH_SUMMED("Distribution Difference by Depth (summed)",
      "Calculate distribution difference from target.\nabs( [ sum(factor[i].bitCount) / (2*(depth+1)) ] - (targetBitCount / targetBitLen) )",
      (n) -> Math.abs(((n.p.bitCount() + n.q.bitCount()) / (2.0 * (1.0+n.depth))) - Solver.cacheSemiprimeBitCountOverBitLen)
  ),

  DIST_EXPECTED_GAUSSIAN("Expected Distribution (gaussian)",
      "Calculate h based upon the likelihood that the current factor bit distribution reflects\nexpectations based upon a standard Gaussian distribution.",
      (n) -> Math.abs(0.5 - (((double)n.p.bitCount()/(double)n.p.bitLength()) + (double)n.q.bitCount()/(double)n.q.bitLength()))
  ),

  HAMMING("Hamming Distance",
      "<a href=\"https://en.wikipedia.org/wiki/Hamming_distance\">Hamming distance</a> to goal.\nfor each bit i in target:\n\tsum( n.product[i] != target[i] )",
      (n) -> (double)Solver.semiprime().and(n.product).bitCount()
  ),
  ;

  private final String name, desc;
  private final Function<Node, Double> function;
  Heuristic(String name, String desc, Function<Node, Double> function)
  {
    this.name = name;
    this.desc = "<html>" + desc.replace("\n","<br>").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;") + "</html>";
    this.function = function;
  }

  @Override public String toString() { return name; }
  public String description() { return desc; }
  public double apply(Node n) { return function.apply(n); }

  public static Heuristic fromFormattedName(String name)
  {
    for (Heuristic h : Heuristic.values()) if (h.toString().equals(name)) return h;
    return null;
  }
}
