package com.entangledloops.heuristicsearch.semiprime;

import com.entangledloops.heuristicsearch.semiprime.client.ClientGui;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by setem on 3/22/16.
 */
public class Experiments
{
  public static void main(String[] args)
  {
    new ClientGui();

    final Random random = new Random(1);
    final Consumer<Solver.Node> callback = (n) -> Log.o(n.toString());

    for (int i = 10; i < 100; ++i)
    {
      Solver.reset(); Solver.callback(callback);

      final BigInteger p = BigInteger.probablePrime(i, random);
      final BigInteger q = BigInteger.probablePrime(i, random);
      final BigInteger pq = p.multiply(q);

      final Solver solver1 = new Solver(pq);
      solver1.start(); solver1.join();

      Solver.pLength(i); Solver.qLength(i);

      final Solver solver2 = new Solver(pq);
      solver2.start(); solver2.join();
    }
  }
}
