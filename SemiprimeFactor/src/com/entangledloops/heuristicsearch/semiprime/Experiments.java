package com.entangledloops.heuristicsearch.semiprime;

import com.entangledloops.heuristicsearch.semiprime.client.ClientGui;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by setem on 3/22/16.
 */
public class Experiments
{
  public static void main(String[] args)
  {
    new ClientGui();

    final Random random = new Random(1);
    for (int i = 10; i < 100; ++i)
    {
      final BigInteger p = BigInteger.probablePrime(i, random);
      final BigInteger q = BigInteger.probablePrime(i, random);
      final BigInteger pq = p.multiply(q);

      Solver.reset();
      final Thread solver1 = Solver.get(pq);
      if (null != solver1) { try { solver1.start(); solver1.join(); } catch (Throwable t) { Log.e(t); } }

      Solver.reset(); Solver.pLength(i); Solver.qLength(i);
      final Thread solver2 = Solver.get(pq);
      if (null != solver2) { try { solver2.start(); solver2.join(); } catch (Throwable t) { Log.e(t); } }
    }
  }
}
