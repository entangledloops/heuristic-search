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
  private final static int              seed   = 1;
  private final static Random           random = new Random(seed);
  private final static SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
  private final static String           prefix = "experiments." + format.format(new Date());
  private final static String           log    = prefix + ".log";
  private final static String           csv    = prefix + ".csv";

  public static void main(String[] args)
  {
    try (final PrintWriter log = new PrintWriter(Experiments.log);
         final PrintWriter csv = new PrintWriter(Experiments.csv))
    {
      // init
      Log.init(s -> { log.write(s); log.flush(); });
      Solver.init(csv); Solver.callback((n) -> {});
      Log.o("log: " + Experiments.log + "\ncsv: " + Experiments.csv + "\nseed: " + seed);

      // run all experiments
      for (int i = 10; i < 25; ++i)
      {
        // prepare a new target
        final BigInteger p = BigInteger.probablePrime(i, random);
        final BigInteger q = BigInteger.probablePrime(i, random);
        final BigInteger s = p.multiply(q);
        Log.o(s + " = " + p + " * " + q);

        // run all desired searches against target
        for (Heuristic heuristic : Heuristic.values())
        {
          Solver.heuristics(heuristic); // set search heuristics
          new Solver(s).start().join(); // execute search
        }

        Solver.release(); // release search memory, don't care about history
      }

      // cleanup
      Log.o("all test completed, exiting");
      Solver.shutdown();
      System.exit(0);
    }
    catch (Throwable t)
    {
      System.err.println( t.getMessage() ); t.printStackTrace();
      System.exit(1);
    }
  }
}
