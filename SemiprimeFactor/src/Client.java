import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Stephen Dunn
 * @since October 31, 2015
 */
public class Client
{
  private final AtomicReference<Socket> socket = new AtomicReference<>();
  private final AtomicReference<Thread> thread = new AtomicReference<>();
  private final AtomicReference<Runnable> connectionEvent = new AtomicReference<>();

  private final AtomicReference<BufferedReader> in = new AtomicReference<>();
  private final AtomicReference<PrintWriter>    out = new AtomicReference<>();

  private final AtomicReference<String> username = new AtomicReference<>();
  private final AtomicReference<String> email = new AtomicReference<>();
  private final AtomicReference<String> ip = new AtomicReference<>();
  private final AtomicReference<String> hostname = new AtomicReference<>();

  private final boolean isRemote;
  private final AtomicBoolean isConnected = new AtomicBoolean(false);

  /**
   * For accepting a new remote client connecting to the server.
   * @param socket
   */
  public Client(Socket socket)
  {
    Log.d("accepting new client connection...");

    try
    {
      this.isRemote = true;
      this.socket.set(socket);
      init();
      Log.d("accepted new client connection: " + ip + " (" + hostname + ")");
    }
    catch (Throwable t) { isConnected.set(false); throw new NullPointerException(); }

    Log.d("connection with " + ip() + " established");
  }

  /**
   * Connects a new local client to a remote server.
   * @param host
   * @param port
   */
  public Client(String host, int port, Runnable connectionEvent) throws NullPointerException
  {
    Log.d("connecting to " + host + ":" + port + "...");

    try
    {
      this.connectionEvent.set(connectionEvent);
      this.isRemote = false;
      this.socket.set(new Socket(host, port));
      init();
    }
    catch (Throwable t) { isConnected.set(false); throw new NullPointerException(); }

    Log.d("connection with " + ip() + " established");
  }

  private void init() throws IOException
  {
    if (!isConnected.compareAndSet(false, true))
    {
      Log.d("duplicate connection error");
      return;
    }

    this.ip.set( socket().getInetAddress().getHostAddress() );
    this.hostname.set( socket().getInetAddress().getCanonicalHostName() );
    this.in.set(new BufferedReader( new InputStreamReader(socket().getInputStream())) );
    this.out.set(new PrintWriter( socket().getOutputStream()) );
    this.thread.set(new Thread( new ClientThread() ));
    this.thread.get().start();
  }

  /**
   * Closes this client connection.
   */
  public void close()
  {
    if (!isConnected.compareAndSet(true, false)) return;
    Log.d("closing connection: " + toString() + "...");

    try { socket().close(); }
    catch (Throwable t) { Log.e(t); }

    if (Thread.currentThread() != thread.get())
    {
      try
      {
        thread.get().interrupt();
        thread.get().join();
      }
      catch (Throwable t) { Log.e(t); }
    }
    else if (!isRemote) connectionEvent.get().run();
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
    Packet packet = Packet.ERROR;
    if (!isConnected.get()) return packet;
    try { packet = Packet.valueOf( readln() ); }
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
    Log.d("receiving a new semiprime target request from " + toString() + "...");

    Log.d("semiprime target sent to " + toString());
  }

  private void sendTarget()
  {
    Log.d("sending a new semiprime target...");

    Log.d("semiprime target sent");
  }

  private void recvUsername()
  {
    try
    {
      final String prev = username();
      username.set( readln() );
      Log.d("client updated username from \"" + prev + "\" to \"" + username + "\"");
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
      Log.d("client updated email from \"" + prev + "\" to \"" + email + "\"");
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
    Log.d("receiving open list update from " + toString() + "...");

    Log.d("open list updated by " + toString());
  }

  private void sendOpenUpdate()
  {
    Log.d("sending open list update...");

    Log.d("open list update sent");
  }

  private void recvOpenCheck()
  {
    Log.d(toString() + " requested open list check...");

    Log.d("open list checked and response sent to " + toString());
  }

  private void sendOpenCheck()
  {
    Log.d("requesting open list check...");

    Log.d("open list checked and response received");
  }

  private void recvOpenMerge()
  {
    Log.d("merging open list with " + toString() + "...");

    Log.d("open list merged with " + toString());
  }

  private void sendOpenMerge()
  {
    Log.d("merging open list...");

    Log.d("open list merged");
  }

  private void recvClosedUpdate()
  {
    Log.d("receiving closed list update from " + toString() + "...");

    Log.d("closed list update received from " + toString());
  }

  private void sendClosedUpdate()
  {
    Log.d("sending closed list update from " + toString() + "...");

    Log.d("closed list update sent");
  }

  private void recvClosedCheck()
  {
    Log.d(toString() + " requested closed list check...");

    Log.d("closed list checked and response sent to " + toString());
  }

  private void sendClosedCheck()
  {
    Log.d("requesting closed list check...");

    Log.d("closed list checked and response received");
  }

  private void recvClosedMerge()
  {
    Log.d("merging closed list with " + toString() + "...");

    Log.d("closed list merged with " + toString());
  }

  private void sendClosedMerge()
  {
    Log.d("merging closed list...");

    Log.d("closed list merged");
  }

  private void recvSolved()
  {
    Log.d(toString() + " claims to have found a solution!");
  }

  private void sendSolved()
  {
    Log.d("sending solution to server...");

    Log.d("solution sent!");
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
        }
      }
      catch (Throwable t) { if (!Thread.interrupted()) Log.e(t); }

      Log.d("client connection closed");
    }
  }

  public static void main(String[] args)
  {
    new ClientGui();
  }
}
