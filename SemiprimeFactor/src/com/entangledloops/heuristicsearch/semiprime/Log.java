package com.entangledloops.heuristicsearch.semiprime;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Log
{
  private static final ReentrantLock    outputLock     = new ReentrantLock(true);
  private static       Consumer<String> outputFunction = null;

  public static void init(Consumer<String> outputFunction)
  {
    Utils.lockAndRun(outputLock, () -> Log.outputFunction = outputFunction);
  }

  public static void d(final String msg)
  {
    System.out.println(msg);
    Utils.lockAndRun(outputLock, () -> outputFunction.accept(msg));
  }

  public static void e(final String msg, final Throwable t)
  {
    if (null != msg) { System.err.println(msg); Utils.lockAndRun(outputLock, () -> outputFunction.accept(msg)); }
    if (null != t) { System.err.println(t.getMessage()); t.printStackTrace(); }
  }
  public static void e(final String msg) { e(msg, null); }
  public static void e(final Throwable t) { e(null, t); }
}
