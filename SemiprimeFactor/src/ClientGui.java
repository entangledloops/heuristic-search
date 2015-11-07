import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Stephen on 11/1/2015.
 */
public class ClientGui extends JFrame implements DocumentListener
{
  private static final String VERSION            = "0.3a";
  private static final String DEFAULT_TITLE      = "Semiprime Factorization Client - v"+VERSION;
  private static final String DOWNLOAD_URL       = "https://github.com/entangledloops/heuristicSearch";
  private static final String ABOUT_URL          = "https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization";
  private static final String LIKE_IM_FIVE_URL   = "https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization---%22I-don't-math%22-edition";
  private static final String SOURCE_URL         = "https://github.com/entangledloops/heuristicSearch/tree/master";
  private static final String HOMEPAGE_URL       = "http://www.entangledloops.com";
  private static final String BTN_CONNECT_STRING = "Connect Now";
  private static final int    DEFAULT_WIDTH      = 800, DEFAULT_HEIGHT = 600;
  private static final int    HISTORY_ROWS       = 5,    HISTORY_COLS   = 20;

  private ImageIcon icnNode, icnCpu, icnNet;
  private JMenuBar  mnuBar;
  private JMenu     mnuFile, mnuAbout;
  private JTabbedPane pneMain;
  private JPanel      pnlNet, pnlConnect, pnlCpu, pnlCpuLeft, pnlCpuRight, pnlNode;
  private JTextArea  txtHistory;
  private JTextField txtUsername, txtEmail, txtAddress;
  private JFormattedTextField txtPort;
  private JSlider             sldProcessors, sldCap, sldMemory, sldIdle;
  private JCheckBox   chkWorkAlways;
  private JButton     btnConnect, btnUpdate;
  private JScrollPane scrollPaneHistory;

  private final AtomicReference<Client> client = new AtomicReference<>(null);
  private final AtomicBoolean isConnecting = new AtomicBoolean(false);
  private final AtomicBoolean isUpdatePending = new AtomicBoolean(false);

  public ClientGui()
  {
    super();
    setVisible(false);

    if (!create())
    {
      Log.e("failed to create the app window");
      return;
    }

    setVisible(true);
    toFront();

    //connect();
  }

  public void resetFrame()
  {
    setTitle(DEFAULT_TITLE);
    setState(Frame.NORMAL);
    setUndecorated(false);
    setResizable(true);
    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    setLocationRelativeTo(getRootPane());
  }

  public void exit()
  {
    Log.d("saving settings and progress...");

    saveSettings();

    Log.d("all settings and progress saved");

    sendWork();

    final Client connection = client.getAndSet(null);
    if (null != connection && connection.connected()) connection.close();

    System.exit(0);
  }

  // re-initializes the window's components
  public boolean create()
  {
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    // Connection tab

    // gather basic info about this device
    final double toGb = 1024.0 * 1024.0 * 1024.0;
    final Runtime runtime = Runtime.getRuntime();
    final String version = Runtime.class.getPackage().getImplementationVersion();
    final int processors = runtime.availableProcessors();

    // initialize all components to default settings
    txtHistory = new JTextArea();

    txtHistory.setRows(HISTORY_ROWS);
    txtHistory.setColumns(HISTORY_COLS);
    txtHistory.setLineWrap(true);
    txtHistory.setWrapStyleWord(true);
    txtHistory.setEditable(false);
    txtHistory.setVisible(true);
    txtHistory.setEnabled(true);

    Log.init(s ->
    {
      txtHistory.append(s + "\n");
      txtHistory.setCaretPosition(txtHistory.getText().length()-1);
    });

    Log.d("\"Thank you\" raised to the 101st power for helping my semiprime research!");
    Log.d("If you're computer cracks a target number, you will be credited in the publication (assuming you provided an email I can reach you at).");
    Log.d("If you're interested in learning exactly what this software does and why, checkout the \"About\" menu.\n");

    scrollPaneHistory = new JScrollPane(txtHistory);
    scrollPaneHistory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPaneHistory.setVisible(true);

    DefaultHighlighter highlighter = new DefaultHighlighter();
    txtHistory.setHighlighter(highlighter);

    // username
    final JLabel lblUsername = new JLabel("Optional username:");
    lblUsername.setHorizontalAlignment(SwingConstants.CENTER);
    txtUsername = new JTextField(System.getProperty("user.name"));
    txtUsername.setHorizontalAlignment(SwingConstants.CENTER);

    // email
    final JLabel lblEmail = new JLabel("Optional email (in case you crack a number\u2014will never share):");
    lblEmail.setHorizontalAlignment(SwingConstants.CENTER);
    txtEmail = new JTextField("nope@take-all-the-credit.com");
    txtEmail.setHorizontalAlignment(SwingConstants.CENTER);

    // host address label and text box
    final JLabel lblAddress = new JLabel("Server address:");
    lblAddress.setHorizontalAlignment(SwingConstants.CENTER);
    txtAddress = new JTextField(Server.DEFAULT_HOST);
    txtAddress.setHorizontalAlignment(SwingConstants.CENTER);
    txtAddress.setColumns(Server.DEFAULT_HOST.length());

    // port box and restrict to numbers
    final JLabel lblPort = new JLabel("Server port:");
    lblPort.setHorizontalAlignment(SwingConstants.CENTER);
    final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
    final DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
    decimalFormat.setGroupingUsed(false);
    txtPort = new JFormattedTextField(decimalFormat);
    txtPort.setHorizontalAlignment(SwingConstants.CENTER);
    txtPort.setColumns(5);
    txtPort.setText(Server.DEFAULT_PORT+"");

    final JLabel lblConnectNow = new JLabel("Click update if you change your username or email after connecting:");
    lblConnectNow.setHorizontalAlignment(SwingConstants.CENTER);

    btnConnect = new JButton(BTN_CONNECT_STRING);
    btnConnect.setHorizontalAlignment(SwingConstants.CENTER);
    btnConnect.addActionListener((event) ->
    {
      if (isConnecting.compareAndSet(false, true)) connect();
    });

    btnUpdate = new JButton("Update");
    btnUpdate.setHorizontalAlignment(SwingConstants.CENTER);
    btnUpdate.setEnabled(false);
    btnUpdate.addActionListener((event) ->
    {
      update();
    });

    final JPanel pnlConnectBtn = new JPanel(new GridLayout(1,2));
    pnlConnectBtn.add(btnConnect);
    pnlConnectBtn.add(btnUpdate);

    // add the components to the left-side connect region
    pnlConnect = new JPanel(new GridLayout(10,1));
    pnlConnect.add(lblUsername);
    pnlConnect.add(txtUsername);
    pnlConnect.add(lblEmail);
    pnlConnect.add(txtEmail);
    pnlConnect.add(lblAddress);
    pnlConnect.add(txtAddress);
    pnlConnect.add(lblPort);
    pnlConnect.add(txtPort);
    pnlConnect.add(lblConnectNow);
    pnlConnect.add(pnlConnectBtn);

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    // setup the icons and menus
    try
    {
      icnNode = new ImageIcon(getClass().getResource("res/icon32x32.png"));
      icnCpu = new ImageIcon(getClass().getResource("res/cpu32x32.png"));
      icnNet = new ImageIcon(getClass().getResource("res/net32x32.png"));
      setIconImage(icnNode.getImage());
    }
    catch (Throwable t) { Log.e(t); }

    mnuBar = new JMenuBar();

    mnuFile = new JMenu("File");
    mnuBar.add(mnuFile);

    final JMenuItem mnuSaveSettings = new JMenuItem("Save Settings");
    mnuSaveSettings.addActionListener((l) -> saveSettings());
    mnuFile.add(mnuSaveSettings);

    final JMenuItem mnuLoadSettings = new JMenuItem("Load Settings");
    mnuLoadSettings.addActionListener((l) -> loadSettings());
    mnuFile.add(mnuLoadSettings);

    mnuFile.addSeparator();

    final JMenuItem mnuSendWork = new JMenuItem("Send All Completed Work");
    mnuSendWork.addActionListener((l) -> sendWork());
    mnuFile.add(mnuSendWork);

    final JMenuItem mnuRecvWork = new JMenuItem("Request a New Node");
    mnuRecvWork.addActionListener((l) -> recvWork());
    mnuFile.add(mnuRecvWork);

    mnuFile.addSeparator();

    final JMenuItem mnuQuit = new JMenuItem("Save & Quit");
    mnuQuit.addActionListener((l) -> exit());
    mnuFile.add(mnuQuit);

    try
    {
      mnuAbout = new JMenu("About");

      final URI aboutURI = new URI(ABOUT_URL);
      final JMenuItem mnuSPF = new JMenuItem("What is Semiprime Factorization?");
      mnuSPF.addActionListener((l) ->
      {
        try { java.awt.Desktop.getDesktop().browse(aboutURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuSPF);

      final URI likeImFiveURI = new URI(LIKE_IM_FIVE_URL);
      final JMenuItem mnuLikeImFive = new JMenuItem("Explain it again, but like I don't any math.");
      mnuLikeImFive.addActionListener((l) ->
      {
        try { java.awt.Desktop.getDesktop().browse(likeImFiveURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuLikeImFive);

      mnuAbout.addSeparator();

      final URI downloadURI = new URI(SOURCE_URL);
      final JMenuItem mnuDownload = new JMenuItem("Download Latest Client");
      mnuDownload.addActionListener((l) ->
      {
        try { java.awt.Desktop.getDesktop().browse(downloadURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuDownload);

      final URI sourceURI = new URI(SOURCE_URL);
      final JMenuItem mnuSource = new JMenuItem("Source Code");
      mnuSource.addActionListener((l) ->
      {
        try { java.awt.Desktop.getDesktop().browse(sourceURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuSource);

      mnuAbout.addSeparator();

      final URI homepageURI = new URI(HOMEPAGE_URL);
      final JMenuItem mnuHomepage = new JMenuItem("My Homepage");
      mnuHomepage.addActionListener((l) ->
      {
        try { java.awt.Desktop.getDesktop().browse(homepageURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuHomepage);

      mnuBar.add(mnuAbout);
    }
    catch (Throwable t) { Log.d("for more info, visit:\n" + ABOUT_URL);  Log.e(t); }

    setJMenuBar(mnuBar);

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    // Node tab
    pnlNode = new JPanel(new GridLayout(1,1));


    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    // CPU tab

    // setup the memory/ processing limit sliders:
    final JLabel lblProcessors = new JLabel("Processors to use:");
    lblProcessors.setHorizontalAlignment(SwingConstants.CENTER);
    sldProcessors = new JSlider(1, processors, processors);
    sldProcessors.setMajorTickSpacing(1);
    sldProcessors.setSnapToTicks(true);
    sldProcessors.setPaintTicks(true);
    sldProcessors.setPaintLabels(true);
    sldProcessors.addChangeListener((c) ->
    {
      if (sldProcessors.getValueIsAdjusting()) return;
      int val = sldProcessors.getValue();
      Log.d("processor cap adjusted: " + val);
    });


    final JLabel lblCap = new JLabel("Per-processor usage (%):");
    lblCap.setHorizontalAlignment(SwingConstants.CENTER);
    sldCap = new JSlider(0, 100, 100);
    sldCap.setMajorTickSpacing(25);
    sldCap.setMinorTickSpacing(5);
    sldCap.setSnapToTicks(true);
    sldCap.setPaintLabels(true);
    sldCap.setPaintTicks(true);
    sldCap.addChangeListener((c) ->
    {
      if (sldCap.getValueIsAdjusting()) return;
      int val = sldCap.getValue();
      Log.d("CPU cap adjusted: " + val + "%");
    });


    final JLabel lblMemory = new JLabel("Memory usage (%):");
    lblMemory.setHorizontalAlignment(SwingConstants.CENTER);
    sldMemory = new JSlider(0, 100, 100);
    sldMemory.setMajorTickSpacing(25);
    sldMemory.setMinorTickSpacing(5);
    sldMemory.setSnapToTicks(true);
    sldMemory.setPaintLabels(true);
    sldMemory.setPaintTicks(true);
    sldMemory.addChangeListener((c) ->
    {
      if (sldMemory.getValueIsAdjusting()) return;
      int val = sldMemory.getValue();
      Log.d("memory cap adjusted: " + val + "%");
    });

    final JLabel lblIdle = new JLabel("Idle time until work begins (min):");
    lblIdle.setHorizontalAlignment(SwingConstants.CENTER);
    sldIdle = new JSlider(0, 30, 5);
    sldIdle.setMajorTickSpacing(5);
    sldIdle.setMinorTickSpacing(1);
    sldIdle.setSnapToTicks(true);
    sldIdle.setPaintLabels(true);
    sldIdle.setPaintTicks(true);
    sldIdle.addChangeListener((c) ->
    {
      if (sldIdle.getValueIsAdjusting()) return;
      int val = sldIdle.getValue();
      Log.d("time until work begins adjusted: " + val + " minutes");
    });

    // setup the left-side:
    pnlCpuLeft = new JPanel(new GridLayout(8,1));
    pnlCpuLeft.add(lblProcessors);
    pnlCpuLeft.add(sldProcessors);
    pnlCpuLeft.add(lblCap);
    pnlCpuLeft.add(sldCap);
    pnlCpuLeft.add(lblMemory);
    pnlCpuLeft.add(sldMemory);
    pnlCpuLeft.add(lblIdle);
    pnlCpuLeft.add(sldIdle);

    // setup the right-side:
    pnlCpuRight = new JPanel(new GridLayout(1,1));

    // setup connect button and "always work" checkbox
    chkWorkAlways = new JCheckBox("Always work, even when I'm not idle.", false);
    chkWorkAlways.setHorizontalAlignment(SwingConstants.CENTER);
    chkWorkAlways.setFocusPainted(false);
    chkWorkAlways.addActionListener((l) ->
    {
      final boolean work = chkWorkAlways.isSelected();
      Log.d("always work: " + (work ? "yes" : "no"));
    });

    pnlCpuRight.add(chkWorkAlways);

    // create the full CPU panel
    pnlCpu = new JPanel(new GridLayout(1,2));
    pnlCpu.add(pnlCpuLeft);
    pnlCpu.add(pnlCpuRight);

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    // add tabs to frame

    // organize them and add them to the panel
    pnlNet = new JPanel(new GridLayout(2,1));
    pnlNet.add(scrollPaneHistory);
    pnlNet.add(pnlConnect);

    // create the tabbed panes
    pneMain = new JTabbedPane();
    pneMain.addTab("", icnNet, pnlNet);
    pneMain.addTab("", icnNode, pnlNode);
    pneMain.addTab("", icnCpu, pnlCpu);

    // add the panel to the frame and show everything
    getContentPane().add(pneMain);
    resetFrame();

    // on close, kill the server connection
    addWindowListener(new java.awt.event.WindowAdapter()
    {
      @Override
      public void windowClosing(final WindowEvent winEvt)
      {
        if (null != client.get()) { try { client.get().close(); } catch (Throwable t) { Log.e(t); } }
        System.exit(0);
      }
    });

    // collect garbage and report memory info
    runtime.gc();
    final double freeMemory = (double)runtime.freeMemory() / toGb;
    final double totalMemory = (double)runtime.totalMemory() / toGb;
    final double maxMemory =  (double)runtime.maxMemory() / toGb;
    final DecimalFormat formatter = new DecimalFormat("#.##");

    Log.d("current java version: " + version + ", required: 1.8+");
    Log.d("note: all memory values reported are relative to the JVM, and were reported immediately after invoking the GC");
    Log.d("free memory: ~" + formatter.format(freeMemory) + " (Gb)");
    Log.d("total memory: ~" + formatter.format(totalMemory) + " (Gb)");
    Log.d("max memory: ~" + formatter.format(maxMemory) + " (Gb)");
    Log.d("free memory / total memory: " + formatter.format(100.0*(freeMemory/totalMemory)) + "%");
    Log.d("total memory / max memory: " + formatter.format(100.0*(totalMemory/maxMemory)) + "%");
    Log.d("available processors: " + processors);

    return true;
  }

  private void connect()
  {
    new Thread(() ->
    {
      try
      {
        btnConnect.setEnabled(false);
        btnUpdate.setEnabled(false);

        // close any old connection
        final Client prev = client.getAndSet(null);
        if (null != prev && prev.connected())
        {
          prev.close();
          isConnecting.set(false);
          btnConnect.setText(BTN_CONNECT_STRING);
          return;
        }
        btnConnect.setText("Connecting...");

        // grab the info from the connect boxes
        String username = txtUsername.getText().trim();
        if ("".equals(username)) username = System.getProperty("user.name");

        final String address = txtAddress.getText().trim();
        if ("".equals(address))
        {
          JOptionPane.showMessageDialog(this, "Please enter a valid server address.");
          throw new NumberFormatException("invalid address");
        }

        int port;
        try
        {
          txtPort.validate();
          final String portString = txtPort.getText();
          port = Integer.parseInt(portString);
          if (port < 1 || port > 65535) throw new NumberFormatException();
        }
        catch (NumberFormatException e)
        {
          JOptionPane.showMessageDialog(this, "Please enter a valid port number.");
          throw new NumberFormatException("invalid port");
        }

        client.set(new Client(username, address, port));

        isConnecting.set(false);
        btnConnect.setText("Disconnect");
        btnUpdate.setEnabled(true);
      }
      catch (NumberFormatException e)
      {
        isConnecting.set(false);
        btnConnect.setText(BTN_CONNECT_STRING);
        btnUpdate.setEnabled(false);
        Log.e("connect failure: " + e.getMessage());
      }
      catch (NullPointerException e)
      {
        isConnecting.set(false);
        btnConnect.setText(BTN_CONNECT_STRING);
        btnUpdate.setEnabled(false);
        JOptionPane.showMessageDialog(this, "Couldn't locate Stephen's desktop at the moment.\n" +
            "Will keep retrying every few minutes, and the prime search will begin independently in the meantime.\n\n" +
            "If you're feeling lucky or want to try a different server, you can retry connecting anytime by hitting the big button.");
      }
      finally
      {
        btnConnect.setEnabled(true);
      }
    }).start();
  }

  private void update()
  {
    if (isConnecting.get()) { Log.e("try updating again after you're connected"); return; }
    if (!isUpdatePending.compareAndSet(false, true)) { Log.e("already working on an update, hang tight..."); return; }
    btnUpdate.setEnabled(false);
    Log.d("sending updated settings to server...");



    Log.d("server has acknowledged your updated settings");
    isUpdatePending.set(false);
    btnUpdate.setEnabled(true);
  }

  public void saveSettings()
  {
    Log.d("saving settings...");

    Log.d("settings saved");
  }

  public void loadSettings()
  {
    Log.d("loading settings...");

    Log.d("settings loaded");
  }

  public void sendWork()
  {
    Log.d("sending all completed work to server...");

    Log.d("server successfully received all completed work");
  }

  public void recvWork()
  {
    Log.d("requesting a new primary node...");

    Log.d("new workload received; restarting search...");
  }

  @Override
  public void insertUpdate(DocumentEvent e)
  {

  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {

  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {

  }
}
