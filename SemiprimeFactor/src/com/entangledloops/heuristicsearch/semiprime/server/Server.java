package com.entangledloops.heuristicsearch.semiprime.server;

import com.entangledloops.heuristicsearch.semiprime.Log;
import com.entangledloops.heuristicsearch.semiprime.Packet;
import com.entangledloops.heuristicsearch.semiprime.client.Client;

import java.net.ServerSocket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Server
{
  // shared vars
  public static final String DEFAULT_HOST = "semiprime.servebeer.com";
  public static final int    DEFAULT_PORT = 12288;

  // server backend
  private final int          port; ///< port hosting was launch on
  private final ServerSocket socket;
  private final AtomicReference<Thread> serverThread = new AtomicReference<>();

  // state vars
  private final Queue<Client> clients          = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean ready            = new AtomicBoolean(false); ///< server ready for new clients?
  private final AtomicBoolean exiting          = new AtomicBoolean(false); ///< are we dying?
  private final TimeUnit      shutdownTimeUnit = TimeUnit.MILLISECONDS;
  private final long          shutdownTimeout  = 60000; ///< total time allowed to kill and join all clients

  //////////////////////////////////////////////////////////////////////////////
  //
  // Server
  //
  //////////////////////////////////////////////////////////////////////////////

  public Server() { this(DEFAULT_PORT, null); }
  public Server(int port, Consumer<Packet> socketEventCallback)
  {
    if (port < 1 || port > 65535) { Log.e("server port is invalid: " + port); throw new NullPointerException(); }

    this.port = port;

    try
    {
      Log.o("launching new socket server..."); // 'launched' msg printed in new thread
      socket = new ServerSocket(port, Integer.MAX_VALUE);
      serverThread.set(new Thread(new ServerThread()));
      serverThread.get().start();
    }
    catch (Throwable t)
    {
      exiting.set(true); ready.set(false);
      Log.e("server launch failure", t);
      throw new NullPointerException();
    }
  }

  @Override public String toString()
  {
    return Stream.of(clients).map(Object::toString).reduce((c1,c2) -> c1 + "\n" + c2).orElse("no connected clients");
  }

  ServerSocket socket() { return socket; }
  public boolean ready() { return ready.get() && !exiting(); }
  public boolean exiting() { return exiting.get(); }

  /**
   * Closes all client connections and exits the server.
   */
  public void close()
  {
    if (!exiting.compareAndSet(false, true)) { Log.e("server already closed"); return; }
    try
    {
      socket.close();
      final Thread thread = serverThread.getAndSet(null);
      if (null != thread && Thread.currentThread() != thread) { thread.join(shutdownTimeUnit.toMillis(shutdownTimeout)); }
    }
    catch (Throwable t) { Log.e("error during server shutdown", t); }
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
      if (!ready.compareAndSet(false, true) || exiting()) { Log.e("duplicate server thread launch?"); return; }

      try
      {
        Log.o("server launched and ready to accept clients");
        while (!exiting() && !Thread.interrupted())
        {
          try { clients.add(new Client(socket.accept())); }
          catch (Throwable t) { if (!exiting() && !Thread.interrupted()) Log.e("client accept failure: " + t); }
        }
      }
      catch (Throwable t) { if (!exiting() && !Thread.interrupted()) Log.e(t); }

      try
      {
        Log.o("server shutting down...");
        serverThread.set(null); exiting.set(true); ready.set(false);
        Client c; while (null != (c = clients.poll())) { if (c.connected()) c.close(); }
        Log.o("server shutdown");
      }
      catch (Throwable t) { Log.e("error during server shutdown", t); }
    }
  }

  public static void main(String[] args)
  {
    final ServerGui serverGui = new ServerGui();
    try { while (!serverGui.ready()) Thread.sleep(250); new Client(); } catch (Throwable t) { Log.e(t); System.exit(-1); }
  }
}
