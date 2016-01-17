package com.entangledloops.heuristicsearch.semiprime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Node implements Serializable
{
  private AtomicBoolean locked; ///< is this node locked by a worker client?
  public  BigInteger      p, q; ///< the partial factors for this node
  private double       g, h, f; ///< the heuristic search values for this node

  public Node()
  {
    locked = new AtomicBoolean(false);

  }
}
