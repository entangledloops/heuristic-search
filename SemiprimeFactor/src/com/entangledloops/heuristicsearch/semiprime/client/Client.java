package com.entangledloops.heuristicsearch.semiprime.client;

import com.entangledloops.heuristicsearch.semiprime.Log;
import com.entangledloops.heuristicsearch.semiprime.Packet;
import com.entangledloops.heuristicsearch.semiprime.server.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Client
{
  // client backend
  private final AtomicReference<Socket> socket       = new AtomicReference<>();
  private final AtomicReference<Thread> clientThread = new AtomicReference<>();

  // i/o
  private final AtomicReference<ObjectInputStream>  in  = new AtomicReference<>();
  private final AtomicReference<ObjectOutputStream> out = new AtomicReference<>();
  private final Consumer<Packet> callback; ///< called every received packet

  // client info
  private final AtomicReference<String> username = new AtomicReference<>();
  private final AtomicReference<String> email    = new AtomicReference<>();
  private final AtomicReference<String> ip       = new AtomicReference<>();
  private final AtomicReference<String> hostname = new AtomicReference<>();

  // state
  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final boolean inbound;

  //////////////////////////////////////////////////////////////////////////////
  //
  // Client (server-side)
  //
  //////////////////////////////////////////////////////////////////////////////

  public Client(Socket socket) { this(socket, null); }
  public Client(Socket socket, Consumer<Packet> socketEventCallback)
  {
    if (null == socket) throw new NullPointerException("null socket");

    this.callback = null != socketEventCallback ? socketEventCallback : new IncomingPacketHandler();
    this.inbound = true;
    this.socket.set(socket);

    try
    {
      Log.o("new inbound connection: " + ip + " (" + hostname + ")...");
      init();
      Log.o("inbound connection established: " + toString());
    }
    catch (Throwable t)
    {
      this.connected.set(false); this.socket.set(null);
      Log.e("inbound connection rejected: " + toString(), t);
      throw new NullPointerException();
    }
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Client (client-side)
  //
  //////////////////////////////////////////////////////////////////////////////

  public Client() { this("127.0.0.1"); } ///< @brief self-connect for testing
  public Client(String host) { this(host, Server.DEFAULT_PORT, null); }
  public Client(String host, int port, Consumer<Packet> socketEventCallback) throws NullPointerException
  {
    if (null == host || "".equals(host.trim())) throw new NullPointerException("invalid hostname: " + host);
    if (port < 1 || port > 65535) throw new NullPointerException("invalid port: " + port);

    this.callback = null != socketEventCallback ? socketEventCallback : new IncomingPacketHandler();
    this.inbound = false;

    try
    {
      Log.o("connecting to " + host + ":" + port + "...");
      this.socket.set(new Socket(host, port));
      init();
      Log.o("outbound connection established: " + toString());
    }
    catch (Throwable t)
    {
      this.connected.set(false); this.socket.set(null);
      Log.e("outbound connection failed: " + toString(), t);
      throw new NullPointerException();
    }
  }

  @Override public String toString()
  {
    final String ip = ip();
    final String hostname = hostname();
    final String username = username();
    final String email = email();
    return (null != ip ? ip : "ip unknown") +
        (null != hostname && !hostname.equals(ip) ? " (" + hostname() + ")" : "") +
        (null != username ? " : [" + username + (null != email ? " : " : "") : "") +
        (null != email ? email + (null != username ? "]" : "") : "");
  }

  /**
   * Closes this client connection.
   */
  public void close()
  {
    if (!connected.compareAndSet(true, false)) return;

    Log.o("closing " + (outbound() ? "outbound" : "inbound") + " connection: " + toString());
    try { socket().close(); } catch (Throwable t) { Log.e(t); }

    final Thread thread = clientThread.getAndSet(null);
    if (null != thread && Thread.currentThread() != thread)
    {
      try { thread.interrupt(); thread.join(); }
      catch (Throwable t) { Log.e(t); }
    }
    else if (null != callback) callback.accept(null);
  }

  private String readln() { return read(String.class); }
  public Packet read() { return read(Packet.class); }
  private <T> T read(Class<T> klass) { try { return klass.cast(in().readObject()); } catch (Throwable t) { return null; } }

  public boolean write(Packet.Type type, Object... data) { return write(new Packet(type, data)); }
  private boolean write(Object obj) { try { out().writeObject(obj); return true; } catch (Throwable t) { return false; } }

  private ObjectInputStream in() { return in.get(); }
  private ObjectOutputStream out() { return out.get(); }

  public String ip() { return ip.get(); }
  public String hostname() { return hostname.get(); }

  public String email() { return email.get(); }
  public String email(String email) { return this.email.getAndSet(email); }

  public String username() { return username.get(); }
  public String username(String username) { return this.username.getAndSet(username); }

  private Socket socket() { return socket.get(); }
  public boolean connected() { return connected.get(); }

  private void init() throws IOException
  {
    if (!connected.compareAndSet(false, true)) { Log.e((outbound() ? "outbound" : "inbound") + " client already connected: " + toString()); return; }

    this.ip.set( socket().getInetAddress().getHostAddress() );
    this.hostname.set( socket().getInetAddress().getCanonicalHostName() );
    this.in.set(new ObjectInputStream( socket().getInputStream() ));
    this.out.set(new ObjectOutputStream( socket().getOutputStream() ));

    final Thread thread = new Thread( new ClientThread() );
    this.clientThread.set(thread); thread.start();
  }

  private boolean inbound() { return inbound; }
  private boolean outbound() { return !inbound(); }

  private class IncomingPacketHandler implements Consumer<Packet>
  {
    @Override public void accept(Packet p)
    {
      if (null == p) { close(); return; }
      switch (p.type())
      {
        case TARGET_UPDATE: { break; }
        case USERNAME_UPDATE: { username( p.string() ); break; }
        case EMAIL_UPDATE: { email( p.string() ); break; }
        case OPEN_UPDATE: { break; }
        case OPEN_CHECK: { break; }
        case CLOSED_UPDATE: { break; }
        case CLOSED_CHECK: { break; }
        case SOLUTION_UPDATE: { break; }
        case ERROR: { break; }
        default: Log.e("unhandled packet type received: " + p.type().name());
      }
    }
  }

  /**
   * Thread that waits for inbound packets and handles them for both local and remote clients.
   */
  private class ClientThread implements Runnable
  {
    @Override public void run()
    {
      Log.o("connection established");
      try { while (connected() && !Thread.interrupted()) callback.accept( read() ); }
      catch (Throwable t) { if (connected() && !Thread.interrupted()) Log.e(t); }
      Log.o("connection closed");
    }
  }

  public static void main(String[] args)
  {
    new ClientGui();
  }
}
