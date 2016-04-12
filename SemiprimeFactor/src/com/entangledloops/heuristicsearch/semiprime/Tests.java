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

  private static class Key
  {
    final BigInteger p, q, s;
    Key(BigInteger p, BigInteger q, BigInteger s) { this.p = p; this.q = q; this.s = s; }
  }

  public static Key key(int len)
  {
    final BigInteger p = BigInteger.probablePrime(len/2, random);
    final BigInteger q = BigInteger.probablePrime(len/2, random);
    final BigInteger s = p.multiply(q);

    Log.o(
        "p\n\n" + p.toString() + "\n\n" + p.toString(2) + "\n\n" +
        "q\n\n" + q.toString() + "\n\n" + q.toString(2) + "\n\n" +
        "s\n\n" + s.toString() + "\n\n" + s.toString(2) + "\n\n"
    );

    return new Key(p, q, s);
  }

  public static boolean heuristics(int minLen, int maxLen, int repeat, Heuristic... heuristics)
  {
    try (final PrintWriter log = new PrintWriter(prefix + "heuristics.log");
         final PrintWriter csv = new PrintWriter(prefix + "heuristics.csv"))
      {
        // init
        Log.init(s -> { log.write(s); log.flush(); });
        Solver.init(csv); Solver.callback((n) -> {});

        // run test for factors w/length i
        for (int i = minLen; i < maxLen; ++i)
        {
          // repeat test j times
          for (int j = 0; j < repeat; ++j)
          {
            // prepare a new target
            final Key key = key(i);

            // run all desired searches against target
            for (Heuristic heuristic : Heuristic.values())
            {
              Solver.heuristics(heuristic); // set search heuristics
              new Solver(key.s).start().join(); // execute search
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
    try (final PrintWriter log = new PrintWriter(Tests.prefix + ".sp.log");
         final PrintWriter csv = new PrintWriter(Tests.prefix + ".sp.csv"))
    {
      Log.init(s -> { log.write(s); log.flush(); });
      double pLen = 0, pCount = 0, qLen = 0, qCount = 0, sLen = 0, sCount = 0;

      csv.write("p count,p len,p count/len,q count,q len,q count/len,s count,s len, s count/len\n");

      for (int i = 0; i < repeat; ++i)
      {
        final Key key = key(len);

        final double curPLen = key.p.bitLength(); pLen += curPLen;
        final double curPCount = key.p.bitCount(); pCount += curPCount;
        final double curQLen = key.q.bitLength(); qLen += curQLen;
        final double curQCount = key.q.bitCount(); qCount += curQCount;
        final double curSLen = key.s.bitLength(); sLen += curSLen;
        final double curSCount = key.s.bitCount(); sCount += curSCount;

        csv.write(
            curPCount + "," + curPLen + "," + curPCount/curPLen + "," +
            curQCount + "," + curQLen + "," + curQCount/curQLen + "," +
            curSCount + "," + curSLen + "," + curSCount/curSLen + "\n"
        );
      }

      csv.write("\navgs\n\np,q,s\n" + pCount/pLen + "," + qCount/qLen + "," + sCount/sLen + "\n");
      csv.flush();

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
    if (!semiprimes(2048, 500)) System.exit(1);
    //if (!heuristics(30, 40, 10, Heuristic.values())) System.exit(2);
  }
}
