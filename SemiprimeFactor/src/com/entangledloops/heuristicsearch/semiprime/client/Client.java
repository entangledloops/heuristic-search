package com.entangledloops.heuristicsearch.semiprime.client;

import com.entangledloops.heuristicsearch.semiprime.Log;
import com.entangledloops.heuristicsearch.semiprime.Packet;
import com.entangledloops.heuristicsearch.semiprime.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.entangledloops.heuristicsearch.semiprime.Packet.ERROR;
import static com.entangledloops.heuristicsearch.semiprime.Packet.valueOf;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Client
{
  // client backend
  private final AtomicReference<Socket>         socket       = new AtomicReference<>();
  private final AtomicReference<Thread>         clientThread = new AtomicReference<>();

  // i/o
  private final AtomicReference<BufferedReader> in  = new AtomicReference<>();
  private final AtomicReference<PrintWriter>    out = new AtomicReference<>();
  private final Consumer<Packet> callback; ///< called every received packet

  // client info
  private final AtomicReference<String>         username     = new AtomicReference<>();
  private final AtomicReference<String>         email        = new AtomicReference<>();
  private final AtomicReference<String>         ip           = new AtomicReference<>();
  private final AtomicReference<String>         hostname     = new AtomicReference<>();

  // state
  private final AtomicBoolean isConnected = new AtomicBoolean(false);
  private final boolean isRemote;


  /**
   * For accepting a new remote client connecting to the server.
   * @param socket remote socket connection
   * @param socketEventCallback packet consumer for socket events
   */
  public Client(Socket socket, Consumer<Packet> socketEventCallback)
  {
    Log.o("accepting new client connection...");

    try
    {
      this.callback = socketEventCallback;
      this.isRemote = true;
      this.socket.set(socket);
      init();
      Log.o("accepted new client connection: " + ip + " (" + hostname + ")");
    }
    catch (Throwable t) { isConnected.set(false); throw new NullPointerException(); }

    Log.o("connection with " + ip() + " established");
  }
  public Client(Socket socket) { this(socket, null); }


  /**
   * Connects a new local client to a remote server.
   */
  public Client() { this(Server.DEFAULT_HOST, Server.DEFAULT_PORT, null); }
  public Client(String host, int port, Consumer<Packet> socketEventCallback) throws NullPointerException
  {
    Log.o("connecting to " + host + ":" + port + "...");

    try
    {
      this.callback = socketEventCallback;
      this.isRemote = false;
      this.socket.set(new Socket(host, port));
      init();
    }
    catch (Throwable t) { isConnected.set(false); throw new NullPointerException(); }

    Log.o("connection with " + ip() + " established");
  }

  private void init() throws IOException
  {
    if (!isConnected.compareAndSet(false, true))
    {
      Log.o("duplicate connection error");
      return;
    }

    this.ip.set( socket().getInetAddress().getHostAddress() );
    this.hostname.set( socket().getInetAddress().getCanonicalHostName() );
    this.in.set(new BufferedReader( new InputStreamReader(socket().getInputStream())) );
    this.out.set(new PrintWriter( socket().getOutputStream()) );
    this.clientThread.set(new Thread( new ClientThread() ));
    this.clientThread.get().start();
  }

  /**
   * Closes this client connection.
   */
  public void close()
  {
    if (!isConnected.compareAndSet(true, false)) return;
    Log.o("closing connection: " + toString() + "...");

    try { socket().close(); }
    catch (Throwable t) { Log.e(t); }

    if (Thread.currentThread() != clientThread.get())
    {
      try
      {
        clientThread.get().interrupt();
        clientThread.get().join();
      }
      catch (Throwable t) { Log.e(t); }
    }
    else if (null != callback) callback.accept(null);
  }

  private Socket socket() { return socket.get(); }
  private BufferedReader in() { return in.get(); }
  private PrintWriter out() { return out.get(); }
  private String readln() throws IOException { return in().readLine(); }
  private void println(String s) throws IOException { out().println(s); out().flush(); }

  public boolean connected() { return isConnected.get(); }
  public String ip() { return ip.get(); }
  public String hostname() { return hostname.get(); }
  public String username() { return username.get(); }
  public String email() { return email.get(); }
  public void setEmail(String email) { this.email.set(email); }
  public void setUsername(String username) { this.username.set(username); }

  /**
   * Receive a packet over the socket.
   * @return
   */
  private Packet recvPacket()
  {
    Packet packet = ERROR;
    if (!isConnected.get()) return packet;
    try { packet = valueOf( readln() ); }
    catch (Throwable t) {} // connection was closed
    return packet;
  }

  /**
   * Send a packet over the socket.
   * @param packet
   * @return
   */
  public boolean sendPacket(Packet packet)
  {
    try { println(packet.name()); }
    catch (Throwable t) { Log.e(t); return false; }

    switch (packet)
    {
      case NEW_TARGET: { sendTarget(); break; }
      case USERNAME_UPDATE: { sendUsername(); break; }
      case EMAIL_UPDATE: { sendEmail(); break; }
      case OPEN_UPDATE: { sendOpenUpdate(); break; }
      case OPEN_CHECK: { sendOpenCheck(); break; }
      case OPEN_MERGE: { sendOpenMerge(); break; }
      case CLOSED_UPDATE: { sendClosedUpdate(); break; }
      case CLOSED_CHECK: { sendClosedCheck(); break; }
      case CLOSED_MERGE: { sendClosedMerge(); break; }
      case SOLVED: { sendSolved(); break; }
      case ERROR:
      default: close();
    }
    return true;
  }

  @Override
  public String toString() { return username + " (" + email() + " / " + ip() + ")"; }

  private void recvTarget()
  {
    Log.o("receiving a new semiprime target request from " + toString() + "...");

    Log.o("semiprime target sent to " + toString());
  }

  private void sendTarget()
  {
    Log.o("sending a new semiprime target...");

    Log.o("semiprime target sent");
  }

  private void recvUsername()
  {
    try
    {
      final String prev = username();
      username.set( readln() );
      Log.o("client updated username from \"" + prev + "\" to \"" + username + "\"");
    }
    catch (Throwable t) { Log.e(t); }
  }

  private void sendUsername()
  {
    try { println( username() ); }
    catch (Throwable t) { Log.e(t); }
  }

  private void recvEmail()
  {
    try
    {
      final String prev = email();
      email.set( readln() );
      Log.o("client updated email from \"" + prev + "\" to \"" + email + "\"");
    }
    catch (Throwable t) { Log.e(t); }
  }

  private void sendEmail()
  {
    try { println( email() ); }
    catch (Throwable t) { Log.e(t); }
  }

  private void recvOpenUpdate()
  {
    Log.o("receiving open list update from " + toString() + "...");

    Log.o("open list updated by " + toString());
  }

  private void sendOpenUpdate()
  {
    Log.o("sending open list update...");

    Log.o("open list update sent");
  }

  private void recvOpenCheck()
  {
    Log.o(toString() + " requested open list check...");

    Log.o("open list checked and response sent to " + toString());
  }

  private void sendOpenCheck()
  {
    Log.o("requesting open list check...");

    Log.o("open list checked and response received");
  }

  private void recvOpenMerge()
  {
    Log.o("merging open list with " + toString() + "...");

    Log.o("open list merged with " + toString());
  }

  private void sendOpenMerge()
  {
    Log.o("merging open list...");

    Log.o("open list merged");
  }

  private void recvClosedUpdate()
  {
    Log.o("receiving closed list update from " + toString() + "...");

    Log.o("closed list update received from " + toString());
  }

  private void sendClosedUpdate()
  {
    Log.o("sending closed list update from " + toString() + "...");

    Log.o("closed list update sent");
  }

  private void recvClosedCheck()
  {
    Log.o(toString() + " requested closed list check...");

    Log.o("closed list checked and response sent to " + toString());
  }

  private void sendClosedCheck()
  {
    Log.o("requesting closed list check...");

    Log.o("closed list checked and response received");
  }

  private void recvClosedMerge()
  {
    Log.o("merging closed list with " + toString() + "...");

    Log.o("closed list merged with " + toString());
  }

  private void sendClosedMerge()
  {
    Log.o("merging closed list...");

    Log.o("closed list merged");
  }

  private void recvSolved()
  {
    Log.o(toString() + " claims to have found a solution!");
  }

  private void sendSolved()
  {
    Log.o("sending solution to server...");

    Log.o("solution sent!");
  }

  /**
   * Thread that waits for incoming packets and handles them for both local and remote clients.
   */
  private class ClientThread implements Runnable
  {
    @Override
    public void run()
    {
      try
      {
        while (connected() && !Thread.interrupted())
        {
          final Packet packet = recvPacket();

          switch (packet)
          {
            case NEW_TARGET: { recvTarget(); break; }
            case USERNAME_UPDATE: { recvUsername(); break; }
            case EMAIL_UPDATE: { recvEmail(); break; }
            case OPEN_UPDATE: { recvOpenUpdate(); break; }
            case OPEN_CHECK: { recvOpenCheck(); break; }
            case OPEN_MERGE: { recvOpenMerge(); break; }
            case CLOSED_UPDATE: { recvClosedUpdate(); break; }
            case CLOSED_CHECK: { recvClosedCheck(); break; }
            case CLOSED_MERGE: { recvClosedMerge(); break; }
            case SOLVED: { recvSolved(); break; }
            case ERROR:
            default: close();
          }

          if (null != callback) callback.accept(packet);
        }
      }
      catch (Throwable t) { if (!Thread.interrupted()) Log.e(t); }

      Log.o("client connection closed");
    }
  }

  public static void main(String[] args)
  {
    new ClientGui();
  }
}
