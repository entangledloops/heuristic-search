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
    final SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
    final String prefix = "experiments." + format.format( new Date() );

    try (final PrintWriter out = new PrintWriter(prefix + ".csv");
         final PrintWriter log = new PrintWriter(prefix + ".log"))
    {
      final int seed = 1;
      final Random random = new Random(seed);

      Log.init(log::write);
      Log.o("using seed: " + seed);

      // write csv header
      out.write("Heuristic(s),p*q,p,q,generated,ignored,expanded,open.size(),closed.size(),maxDepth,avgDepth,depth,h,hashCode,product,p,q\n");

      // execute all tests
      for (int i = 10; i < 30; ++i)
      {
        final BigInteger p = BigInteger.probablePrime(i, random);
        final BigInteger q = BigInteger.probablePrime(i, random);
        final BigInteger pq = p.multiply(q);

        for (Heuristic heuristic : Heuristic.values())
        {
          out.write(heuristic.toString() + "," + pq + "," + p + "," + q + ",");

          // prepare the solver for a new search
          Solver.reset();
          Solver.callback((n) -> out.write((null != n ? n.toCsv() : "no goal found") + "\n"));
          Solver.heuristics(heuristic);

          // execute search
          new Solver(pq).start().join();

          out.flush();
        }
      }

      Log.o("all test completed, exiting");
      System.exit(0);
    }
    catch (Throwable t)
    {
      System.err.println(t.getMessage()); t.printStackTrace();
      System.exit(1);
    }
  }
}
