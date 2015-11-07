import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Created by Stephen on 10/31/2015.
 */
public class Log
{
  private static final ReentrantLock    outputLock     = new ReentrantLock(true);
  private static       Consumer<String> outputFunction = null;

  public static void init(Consumer<String> outputFunction)
  {
    Log.outputFunction = outputFunction;
  }

  private static void writeToLog(final String msg)
  {
    if (null != outputFunction)
    {
      outputLock.lock();
      try
      {
        if (outputLock.getHoldCount() > 1) return;
        outputFunction.accept(msg);
      }
      catch (Throwable t) { Log.e(t); }
      finally { outputLock.unlock(); }
    }
  }

  public static void d(final String msg)
  {
    System.out.println(msg);
    writeToLog(msg);
  }

  public static void e(final String msg, final Throwable t)
  {
    if (null != msg) { System.err.println(msg); writeToLog(msg); }
    if (null != t) { System.err.println(t.getMessage()); t.printStackTrace(); writeToLog(t.getMessage()); }
  }
  public static void e(final String msg) { e(msg, null); }
  public static void e(final Throwable t) { e(null, t); }
}
