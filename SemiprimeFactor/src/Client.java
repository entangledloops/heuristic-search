import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Stephen on 10/31/2015.
 */
public class Client
{
  private final Socket socket;
  private final Thread thread;

  private final BufferedReader in;
  private final PrintWriter    out;

  private String username, email;
  private boolean isExiting   = false;
  private boolean isConnected = false;

  /**
   * For accepting a new remote client connecting to the server.
   * @param socket
   */
  public Client(Socket socket)
  {
    try
    {
      Log.d("accepting new client connection: " + socket.getInetAddress().getCanonicalHostName() + " (" + socket.getInetAddress().getHostAddress() + ")");
      this.socket = socket;
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.out = new PrintWriter(socket.getOutputStream());
      this.thread = new Thread(new RemoteClientThread());
      this.thread.start();
    }
    catch (Throwable t) { throw new NullPointerException(); }

    Log.d("connection with " + socket.getInetAddress().getHostAddress() + " established");
  }

  /**
   * Connects a new local client to a remote server.
   * @param host
   * @param port
   */
  public Client(String host, int port) throws NullPointerException
  {
    Log.d("connecting to " + host + ":" + port + "...");

    try
    {
      this.socket = new Socket(host, port);
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.out = new PrintWriter(socket.getOutputStream());
      this.thread = new Thread(new LocalClientThread());
      this.thread.start();
    }
    catch (Throwable t) { throw new NullPointerException(); }

    Log.d("connected successfully");
  }

  /**
   * Closes this client connection.
   */
  public void close()
  {
    Log.d("closing client connection...");
    isExiting = true;

    try { socket.close(); }
    catch (Throwable t) { Log.e(t); }

    try { thread.interrupt(); thread.join(); }
    catch (Throwable t) { Log.e(t); }
  }

  public boolean connected() { return isConnected; }

  /**
   * Receive a packet from a remote client.
   * @param packet
   * @return
   */
  private boolean RemoteRecvPacket(Packet packet)
  {
    return true;
  }

  /**
   * Send a packet to a remote client.
   * @param packet
   * @return
   */
  private boolean RemoteSendPacket(Packet packet)
  {
    return true;
  }

  private class RemoteClientThread implements Runnable
  {
    @Override
    public void run()
    {
      try
      {
        while (!isExiting && !Thread.interrupted())
        {

        }
      }
      catch (Throwable t) { if (!Thread.interrupted()) Log.e(t); }

      Log.d("remote client connection closed");
    }
  }

  /**
   * Receive a packet from the server.
   * @param packet
   * @return
   */
  private boolean LocalRecvPacket(Packet packet)
  {
    return true;
  }

  /**
   * Send a packet to the server.
   * @param packet
   * @return
   */
  private boolean LocalSendPacket(Packet packet)
  {
    return true;
  }

  private class LocalClientThread implements Runnable
  {
    @Override
    public void run()
    {
      isConnected = true;

      try
      {
        while (!isExiting && !Thread.interrupted())
        {

        }
      }
      catch (Throwable t) { if (!Thread.interrupted()) Log.e(t); }

      try { socket.close(); }
      catch (Throwable t) { Log.e(t); }

      Log.d("connection to server closed");
    }
  }

  public static void main(String[] args)
  {
    new ClientGui();
  }
}
