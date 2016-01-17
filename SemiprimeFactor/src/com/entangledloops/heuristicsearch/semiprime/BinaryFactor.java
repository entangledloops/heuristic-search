package com.entangledloops.heuristicsearch.semiprime;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Stephen Dunn
 * @since November 2, 2015
 */
public class BinaryFactor
{
  private final BigInteger target;       ///< the target semiprime value
  private final String     targetString; ///< as a binary string (stored for efficiency)
  private final int        targetLen;    ///< the binary string length (stored for efficiency)

  private final ConcurrentHashMap<String, Node> open = new ConcurrentHashMap<>();
  private AtomicReference<BigInteger> openSize = new AtomicReference<>(BigInteger.ZERO);
  private AtomicReference<BigInteger> generated = new AtomicReference<>(BigInteger.ZERO);
  private AtomicReference<BigInteger> expanded = new AtomicReference<>(BigInteger.ZERO);

  public BinaryFactor(final String target)
  {
    this.target = new BigInteger(target, 10);
    this.targetString = this.target.toString(2);
    this.targetLen = this.targetString.length();
  }

  public Node factor()
  {
    Log.d("target semiprime (base 10): " + target.toString() + "\ntarget semiprime (base 2):  " + targetString);
    Log.d("length (base 10): " + target.toString().length() + " digits\nlength (base 2):  " + targetLen + " digits");
    return null;
  }

  private Node expand(final Node n)
  {
    return null;
  }

  private boolean isGoal(Node n)
  {
    return target.equals(n.p.multiply(n.q));
  }
}
