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
  private static       Consumer<String> outputCallback = null;

  public static void init(Consumer<String> outputCallback)
  {
    Log.outputCallback = outputCallback;
  }

  public static void d(final String s)
  {
    if (null != s) System.out.println(s); else e(null, null);
    if (null != outputCallback) Utils.lockAndRun(outputLock, () -> outputCallback.accept(s));
  }

  public static void e(final String s, final Throwable t)
  {
    final String msg = (null != s && s.trim().length() > 0 ? s.trim() : "") + (null != t ? t.getMessage() : "");
    System.err.println(!"".equals(msg) ? msg : "empty error reported");
    if (null != t) t.printStackTrace();
    Utils.lockAndRun(outputLock, () -> outputCallback.accept(msg));
  }
  public static void e(final String msg) { e(msg, null); }
  public static void e(final Throwable t) { e(null, t); }
}
