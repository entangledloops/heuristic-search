import java.net.ServerSocket;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Server
{
  //////////////////////////////////////////////////////////////////////////////
  //
  // User prefs
  //
  //////////////////////////////////////////////////////////////////////////////

  private static final String   DEFAULT_HOST          = "semiprime.servebeer.com";
  private static final int      DEFAULT_PORT          = 12288;
  private static final TimeUnit SHUTDOWN_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final long     SHUTDOWN_TIMEOUT      = 5000;

  private final ServerSocket serverSocket;
  private final Queue<Client> clientSockets = new ConcurrentLinkedQueue<>();
  private final Thread serverThread = new Thread(new ServerThread());

  private final AtomicBoolean isReady   = new AtomicBoolean(false);
  private final AtomicBoolean isExiting = new AtomicBoolean(false);

  /**
   * Initializes the socket server to receive incoming client connections.
   * @param port
   * @return
   */
  public Server(int port) throws NullPointerException
  {
    Log.d("launching new socket server...");
    try
    {
      serverSocket = new ServerSocket(port, Integer.MAX_VALUE);
      serverThread.start();
    }
    catch (Throwable t) { Log.e(t); throw new NullPointerException(t.getMessage()); }
  }
  public Server() throws NullPointerException { this(DEFAULT_PORT); }

  /**
   * Closes all client connections and exits the server.
   */
  public void destroy()
  {
    if (!isExiting.compareAndSet(false,true)) return;

    Log.d("shutting down the server socket...");

    try { serverSocket.close(); }
    catch (Throwable t) { Log.e(t); }

    try { serverThread.join(SHUTDOWN_TIMEOUT_UNIT.toMillis(SHUTDOWN_TIMEOUT)); }
    catch (Throwable t)
    {
      Log.e(t);
      try { serverSocket.close(); }
      catch (Throwable t2) { Log.e(t2); }
    }

    Log.d("socket server shutdown");
  }

  /**
   * Handles incoming client connections.
   */
  private class ServerThread implements Runnable
  {
    @Override
    public void run()
    {
      if (!isReady.compareAndSet(false,true)) return;
      Log.d("server thread launched and waiting for connections");

      try
      {
        while (!isExiting.get() && !Thread.interrupted())
        {
          clientSockets.add(new Client(serverSocket.accept()));
        }
      }
      catch (Throwable t) { if (!isExiting.get()) Log.e(t); }

      try
      {
        final Iterator<Client> iterator = clientSockets.iterator();
        if (iterator.hasNext()) Log.d("closing all client connections...");
        while (iterator.hasNext()) { iterator.next().close(); iterator.remove(); }
      }
      catch (Throwable t) { Log.e(t); }

      isReady.set(false);
      Log.d("server socket closed");
    }
  }

  public static void main(String[] args)
  {
    new ServerGui();
    new BinaryFactor("8605230192532870349").factor();
    /*
    try
    {
      //while (!server.isReady) { try { Thread.sleep(250); } catch (Throwable t) { Log.e(t); } }
      //try { new Client(); } catch (Throwable t) { Log.e(t); }
      //try { Thread.sleep(250); } catch (Throwable t) { Log.e(t); }
    }
    catch (Throwable t) { Log.e(t); }
    */
  }
}
