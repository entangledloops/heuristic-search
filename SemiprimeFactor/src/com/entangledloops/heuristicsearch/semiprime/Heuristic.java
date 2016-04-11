package com.entangledloops.heuristicsearch.semiprime;

import java.util.function.BiFunction;

/**
  * @author Stephen Dunn
  * @since March 20, 2016
  */
public enum Heuristic
{
  NONE("None", "Empty heuristic", (s,n) -> 0.0),

  DIST_EXPECTED_SEPARATE("Expected Distribution (separate)",
      "Calculate distribution difference from target.\nabs( sum(factor[i].bitCount() / factor[i].bitLength()) - (targetBitCount / targetBitLen) )",
      (s,n) -> Math.abs((((double) n.p.bitCount() / (double) n.p.bitLength()) + ((double) n.q.bitCount() / (double) n.q.bitLength())) - s.cacheSemiprimeBitCountOverBitLen)),

  DIST_EXPECTED_SUMMED("Expected Distribution (summed)",
      "Calculate h based upon the likelihood that the current factor bit distribution reflects\nexpectations based upon objective experimental results w/semiprime numbers.",
      (s,n) -> Math.abs(((double) (n.p.bitCount() + n.q.bitCount())/((double) (n.p.bitLength() + n.q.bitLength()))) - s.cacheSemiprimeBitCountOverBitLen)),

  DIST_DIFF_BY_DEPTH_SEPARATE("Distribution Difference by Depth (separate)",
      "Calculate distribution difference from target.\nabs( [ ((p.bitCount/(1+depth))+(q.bitCount/(1+depth)) ] - (targetBitCount/targetBitLen) )",
      (s,n) -> Math.abs( (((double) n.p.bitCount()/(1.0+n.depth)) + ((double) n.q.bitCount()/(1.0+n.depth))) - s.cacheSemiprimeBitCountOverBitLen)),

  DIST_DIFF_BY_DEPTH_SUMMED("Distribution Difference by Depth (summed)",
      "Calculate distribution difference from target.\nabs( [ sum(factor[i].bitCount) / (2*(depth+1)) ] - (targetBitCount / targetBitLen) )",
      (s,n) -> Math.abs(((n.p.bitCount() + n.q.bitCount()) / (2.0 * (1.0+n.depth))) - s.cacheSemiprimeBitCountOverBitLen)),

  DIST_EXPECTED_GAUSSIAN("Expected Distribution 50% (separate)",
      "Calculate h based upon the likelihood that the current factor bit distribution reflects\nexpectations based upon a standard Gaussian distribution.",
      (s,n) -> Math.abs(0.5 - (((double) n.p.bitCount()/(double) n.p.bitLength()) + (double) n.q.bitCount()/(double) n.q.bitLength()))),

  HAMMING("Hamming Distance",
      "<a href=\"https://en.wikipedia.org/wiki/Hamming_distance\">Hamming distance</a> to goal.\nfor each bit i in target:\n\tsum( n.product[i] != target[i] )",
      (s,n) -> (double) s.cacheSemiprime.xor(n.product).bitCount()),
  ;

  private final String name, desc;
  private final BiFunction<Solver, Solver.Node, Double> function;
  Heuristic(String name, String desc, BiFunction<Solver, Solver.Node, Double> function)
  {
    this.name = name;
    this.desc = "<html>" + desc.replace("\n","<br>").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;") + "</html>";
    this.function = function;
  }

  @Override public String toString() { return name; }
  public String description() { return desc; }
  public double apply(Solver s, Solver.Node n) { return function.apply(s,n); }

  public static Heuristic fromFormattedName(String name)
  {
    for (Heuristic h : Heuristic.values()) if (h.toString().equals(name)) return h;
    return null;
  }
}
