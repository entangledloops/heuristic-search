package com.entangledloops.heuristicsearch.semiprime.server;

import com.entangledloops.heuristicsearch.semiprime.Solver;
import com.entangledloops.heuristicsearch.semiprime.Log;
import com.entangledloops.heuristicsearch.semiprime.client.Client;

import java.math.BigInteger;
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
  public static final String   DEFAULT_HOST          = "semiprime.servebeer.com";
  public static final int      DEFAULT_PORT          = 12288;

  private static final TimeUnit shutdownTimeUnit = TimeUnit.MILLISECONDS;
  private static final long     shutdownTimeout  = 5000;

  private final ServerSocket  socket;
  private final Thread        serverThread = new Thread(new ServerThread());
  private final Queue<Client> clients      = new ConcurrentLinkedQueue<>();

  private final AtomicBoolean ready   = new AtomicBoolean(false);
  private final AtomicBoolean exiting = new AtomicBoolean(false);

  //////////////////////////////////////////////////////////////////////////////
  //
  // Server
  //
  //////////////////////////////////////////////////////////////////////////////

  public Server() { this(DEFAULT_PORT); }
  public Server(int port)
  {
    Log.d("launching new socket server...");
    try
    {
      socket = new ServerSocket(port, Integer.MAX_VALUE);
      serverThread.start();
    }
    catch (Throwable t) { Log.e(t); throw new NullPointerException(t.getMessage()); }
  }

  public ServerSocket socket() { return socket; }
  public boolean ready() { return ready.get(); }
  public boolean exiting() { return exiting.get(); }

  /**
   * Closes all client connections and exits the server.
   */
  public void destroy()
  {
    if (!exiting.compareAndSet(false,true)) return;

    Log.d("shutting down the server socket...");

    try { socket.close(); }
    catch (Throwable t) { Log.e(t); }

    try { serverThread.join(shutdownTimeUnit.toMillis(shutdownTimeout)); }
    catch (Throwable t)
    {
      Log.e(t);
      try { socket.close(); }
      catch (Throwable t2) { Log.e(t2); }
    }

    Log.d("socket server shutdown");
  }

  //////////////////////////////////////////////////////////////////////////////
  //
  // Helper classes
  //
  //////////////////////////////////////////////////////////////////////////////


  /**
   * Handles incoming client connections.
   */
  private class ServerThread implements Runnable
  {
    @Override public void run()
    {
      if (!ready.compareAndSet(false,true)) return;
      Log.d("server thread launched and waiting for connections");

      try
      {
        while (!exiting() && !Thread.interrupted())
        {
          clients.add(new Client(socket.accept()));
        }
      }
      catch (Throwable t) { if (!exiting()) Log.e(t); }

      try
      {
        final Iterator<Client> iterator = clients.iterator();
        if (iterator.hasNext()) Log.d("closing all client connections...");
        while (iterator.hasNext()) { iterator.next().close(); iterator.remove(); }
      }
      catch (Throwable t) { Log.e(t); }

      ready.set(false);
      Log.d("server socket closed");
    }
  }

  public static void main(String[] args)
  {
    //rsa-220 = "2260138526203405784941654048610197513508038915719776718321197768109445641817966676608593121306582577250631562886676970448070001811149711863002112487928199487482066070131066586646083327982803560379205391980139946496955261";
    //"8605230192532870349";
    //9141272051 = 95617*95603
    final String semiprime10 = "8605230192532870349";
    final BigInteger semiprime = new BigInteger(semiprime10);
    final Solver solver = Solver.newInstance(semiprime10);
    if (null == solver) { Log.e("failed to create the solver task for semiprime:\n" + semiprime10); System.exit(1); }

    //solver.processors(1);
    //solver.primeLen1(semiprime.toString(2).length()/2);
    //solver.primeLen2(semiprime.toString(2).length()/2);
    solver.callback(n -> {
        if (null == n) { Log.d("no factors were found, are you sure the input is semiprime?"); return; }
        Log.d("\nfactors found: " +
            "\n\tsp10: " + semiprime.toString(10) + " / sp2: " + semiprime.toString(2) +
            "\n\tp10: " + n.p(0, 10) + " / p2: " + n.p(0, 2) +
            "\n\tq10: " + n.p(1, 10) + " / q2: " + n.p(1, 2));
    });

    solver.run();

//    final Server server = new Server();
//    final ServerGui serverGui = new ServerGui(server);
//    try
//    {
//      while (!server.ready()) { try { Thread.sleep(250); } catch (Throwable t) { Log.e(t); } }
//      try { new Client(); } catch (Throwable t) { Log.e(t); }
//      try { Thread.sleep(250); } catch (Throwable t) { Log.e(t); }
//    }
//    catch (Throwable t) { Log.e(t); }

  }
}
