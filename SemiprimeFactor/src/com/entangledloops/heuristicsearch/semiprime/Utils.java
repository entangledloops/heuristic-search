package com.entangledloops.heuristicsearch.semiprime;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.locks.Lock;

/**
 * @author Stephen Dunn
 * @since January 16, 2016
 */
public class Utils
{
  public static boolean lockAndRun(Lock lock, Runnable task)
  {
    if (null == lock || null == task) return false;
    lock.lock();
    try { task.run(); return true; }
    catch (Throwable t) { Log.e(t); return false; }
    finally { lock.unlock(); }
  }

  public static boolean jar()
  {
    try { return Utils.class.getResource("/" + Utils.class.getName().replace('.','/') + ".class").toString().startsWith("jar"); }
    catch (Throwable t) { return false; }
  }

  public static URL getResource(String resource)
  {
    return Utils.class.getClassLoader().getResource(resource);
  }

  public static InputStream getResourceFromJar(String resource)
  {
    return Utils.class.getResourceAsStream("/" + resource);
  }
}
