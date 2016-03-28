package com.entangledloops.heuristicsearch.semiprime;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by setem on 3/22/16.
 */
public class Experiments
{
  public static void main(String[] args)
  {
    Log.disable();

    final int seed = 1;
    final String csv = "experiments." + (new SimpleDateFormat("MM-dd-yyyy").format(new Date())) + ".csv";

    System.out.println("using seed: " + seed + "\nwriting csv: " + csv);

    try (PrintWriter out = new PrintWriter(csv))
    {
      final Random random = new Random(seed);
      Solver.callback((n) -> out.write((null != n ? n.toCsv() : "no goal found") + "\n"));
      out.write("Heuristic(s),p*q,p,q,generated,ignored,expanded,open.size(),closed.size(),maxDepth,avgDepth,depth,h,hashCode,product,p,q\n");

      for (int i = 10; i < 20; ++i)
      {
        final BigInteger p = BigInteger.probablePrime(i, random);
        final BigInteger q = BigInteger.probablePrime(i, random);
        final BigInteger pq = p.multiply(q);

        for (Heuristic heuristic : Heuristic.values())
        {
          Solver.heuristics(heuristic);
          out.write(heuristic.toString() + "," + pq + "," + p + "," + q + ",");
          new Solver(pq).start().join();
        }
      }
    }
    catch (Throwable t) { Log.e(t); System.exit(-1); }
  }
}
