import java.util.concurrent.locks.Lock;

/**
 * @author Stephen Dunn
 * @since January 16, 2016
 */
public class Utils
{
  public static boolean lockAndRun(Lock lock, Runnable run)
  {
    lock.lock();
    try { run.run(); return true; }
    catch (Throwable t) { Log.e(t); return false; }
    finally { lock.unlock(); }
  }
}
