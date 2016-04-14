package com.entangledloops.heuristicsearch.semiprime;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by setem on 3/22/16.
 */
public class Test
{
  private final static int              seed   = 1;
  private final static Random           random = new Random(seed);
  private final static SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
  private final static String           prefix = "test/" + format.format(new Date()) + ".seed-" + seed;

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

    Log.o("[" +
        "\np\n\n" + p.toString() + "\n\n" + p.toString(2) + "\n" +
        "\nq\n\n" + q.toString() + "\n\n" + q.toString(2) + "\n" +
        "\ns\n\n" + s.toString() + "\n\n" + s.toString(2) + "\n" +
        "]\n");

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

  private static int[] runs(BigInteger i)
  {
    final int runs[] = new int[ i.bitLength() ];
    for (int j = 0; j < runs.length; ++j) runs[j] = 0;

    int cur = 0;
    for (int j = 0; j < runs.length; ++j)
    {
      if (i.testBit(j)) ++cur;
      else if (cur > 0) { ++runs[cur-1]; cur = 0; }
    }
    if (cur != 0) ++runs[cur-1];

    return runs;
  }

  public static boolean semiprimes(int len, int repeat)
  {
    try (final PrintWriter log = new PrintWriter(Test.prefix + ".sp.log");
         final PrintWriter csv = new PrintWriter(Test.prefix + ".sp.csv"))
    {
      Log.init(log::write);
      double pLen = 0, pCount = 0, qLen = 0, qCount = 0, sLen = 0, sCount = 0;

      for (int i = 0; i < 5; ++i) csv.write("run,p,q,s,");
      csv.write("\n");

      for (int i = 0; i < 3; ++i) csv.write("count,");
      for (int i = 0; i < 3; ++i) csv.write("len,");
      for (int i = 0; i < 3; ++i) csv.write("count/len,");
      for (int i = 0; i < 3; ++i) csv.write("max run,");
      for (int i = 0; i < 3; ++i) csv.write("max run count,");
      csv.write("\n");

      final long pRuns[] = new long[len/2]; for (int i = 0; i < pRuns.length; ++i) pRuns[i] = 0;
      final long qRuns[] = new long[len/2]; for (int i = 0; i < qRuns.length; ++i) qRuns[i] = 0;
      final long sRuns[] = new long[len]; for (int i = 0; i < sRuns.length; ++i) sRuns[i] = 0;

      for (int i = 0; i < repeat; ++i)
      {
        Log.o(i + ":\n");
        final Key key = key(len);

        // track number of identical consecutive set bits of each possible length in primes and their product
        int maxPRunIndex = 1, maxQRunIndex = 1, maxSRunIndex = 1;
        final int[] curPRuns = runs(key.p); for (int j = 1; j < curPRuns.length; ++j) { pRuns[j] += curPRuns[j]; if (curPRuns[j] > curPRuns[maxPRunIndex]) maxPRunIndex = j; }
        final int[] curQRuns = runs(key.q); for (int j = 1; j < curQRuns.length; ++j) { qRuns[j] += curQRuns[j]; if (curQRuns[j] > curQRuns[maxQRunIndex]) maxQRunIndex = j; }
        final int[] curSRuns = runs(key.s); for (int j = 1; j < curSRuns.length; ++j) { sRuns[j] += curSRuns[j]; if (curSRuns[j] > curSRuns[maxSRunIndex]) maxSRunIndex = j; }

        // calculate some stats
        final double curPLen = key.p.bitLength(); pLen += curPLen;
        final double curPCount = key.p.bitCount(); pCount += curPCount;
        final double curQLen = key.q.bitLength(); qLen += curQLen;
        final double curQCount = key.q.bitCount(); qCount += curQCount;
        final double curSLen = key.s.bitLength(); sLen += curSLen;
        final double curSCount = key.s.bitCount(); sCount += curSCount;

        csv.write(
            1+i + "," +
            curPCount + "," + curQCount + "," + curSCount + "," +
            curPLen + "," + curQLen + "," + curSLen + "," +
            curPCount/curPLen + "," + curQCount/curQLen + "," + curSCount/curSLen + "," +
            1+maxPRunIndex + "," + 1+maxQRunIndex + "," + 1+maxSRunIndex + "," +
            curPRuns[maxPRunIndex] + "," + curQRuns[maxQRunIndex] + "," + curSRuns[maxSRunIndex] + "\n"
        );
      }

      csv.write("\n,avg\np,q,s\n" + pCount/pLen + "," + qCount/qLen + "," + sCount/sLen + "\n");
      csv.write("\n\n,sum / run\nrun,p,q,s\n"); for (int i = 1; i < pRuns.length; ++i) csv.write(1+i + "," + pRuns[i] + "," + qRuns[i] + "," + sRuns[i] + "\n");

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
    try { new File("test").mkdir(); } catch (Throwable ignored) {}
    if (!semiprimes(2048, 1000)) System.exit(1);
    if (!heuristics(30, 40, 10, Heuristic.values())) System.exit(2);
  }
}
