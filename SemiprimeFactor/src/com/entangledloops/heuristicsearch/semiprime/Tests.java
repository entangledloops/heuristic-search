package com.entangledloops.heuristicsearch.semiprime;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by setem on 3/22/16.
 */
public class Tests
{
  private final static int              seed   = 1;
  private final static Random           random = new Random(seed);
  private final static SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
  private final static String           prefix = "tests/" + format.format(new Date()) + ".seed-" + seed;
  private final static String           log    = prefix + ".log";
  private final static String           csv    = prefix + ".csv";
  private final static String           dist   = prefix + ".dist"; ///< semiprime distribution test

  public static BigInteger semiprime(int len)
  {
    final BigInteger p = BigInteger.probablePrime(len/2, random);
    final BigInteger q = BigInteger.probablePrime(len/2, random);
    final BigInteger s = p.multiply(q);
    Log.o(s + " = " + p + " * " + q);
    return s;
  }

  public static boolean heuristics(int minLen, int maxLen, int repeat, Heuristic... heuristics)
  {
    try (final PrintWriter log = new PrintWriter(Tests.log);
         final PrintWriter csv = new PrintWriter(Tests.csv))
      {
        // init
        Log.init(s -> { log.write(s); log.flush(); });
        Solver.init(csv); Solver.callback((n) -> {});
        Log.o("log: " + Tests.log + "\ncsv: " + Tests.csv + "\nseed: " + seed);

        // run test for factors w/length i
        for (int i = minLen; i < maxLen; ++i)
        {
          // repeat test j times
          for (int j = 0; j < repeat; ++j)
          {
            // prepare a new target
            final BigInteger s = semiprime(i);

            // run all desired searches against target
            for (Heuristic heuristic : Heuristic.values())
            {
              Solver.heuristics(heuristic); // set search heuristics
              new Solver(s).start().join(); // execute search
            }

            // release search memory, don't care about history
            Solver.release();
          }

          csv.write( Solver.csvHeader() );
        }

        // cleanup
        Log.o("all tests completed, exiting");
        Solver.shutdown();
        return true;
      }
      catch (Throwable t)
      {
        System.err.println( t.getMessage() ); t.printStackTrace();
        return false;
      }
  }

  public static boolean semiprimes(int len, int repeat)
  {
    try (final PrintWriter dist = new PrintWriter(Tests.dist))
    {
      Log.init(null);
      double bits = 0, setBits = 0;

      for (int i = 0; i < repeat; ++i)
      {
        final BigInteger s = semiprime(len);
        final double curBits = s.bitLength();
        final double curSet = s.bitCount();

        bits += curBits; setBits += curSet;
        dist.write(curSet + ", " + (setBits/bits)); dist.flush();
      }

      Log.o("final avg: " + (setBits / bits));
      return true;
    }
    catch (Throwable t)
    {
      System.err.println( t.getMessage() ); t.printStackTrace();
      return false;
    }
  }

  public static void main(String[] args)
  {
    if (!semiprimes(2048, 100)) System.exit(1);
    if (!heuristics(30, 40, 10, Heuristic.values())) System.exit(2);
  }
}
