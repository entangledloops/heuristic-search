package com.entangledloops.heuristicsearch.semiprime.client;

import com.entangledloops.heuristicsearch.semiprime.Log;
import com.entangledloops.heuristicsearch.semiprime.Packet;
import com.entangledloops.heuristicsearch.semiprime.Solver;
import com.entangledloops.heuristicsearch.semiprime.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

/**
 * @author Stephen Dunn
 * @since November 1, 2015
 */
public class ClientGui extends JFrame implements DocumentListener
{
  //////////////////////////////////////////////////////////////////////////////
  //
  // constants
  //
  //////////////////////////////////////////////////////////////////////////////

  private static final String VERSION          = "0.3a";
  private static final String ICON_NODE_SMALL  = "node16x16.png";
  private static final String ICON_NODE        = "node32x32.png";
  private static final String ICON_CPU         = "cpu32x32.png";
  private static final String ICON_NET         = "net32x32.png";
  private static final String ICON_SETTINGS    = "settings32x32.png";
  private static final String DEFAULT_TITLE    = "Semiprime Factorization Client v" + VERSION;
  private static final String DEFAULT_EMAIL    = "nope@take-all-the-credit.com";
  private static final String ABOUT_URL        = "https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization";
  private static final String NO_MATH_URL      = ABOUT_URL + "---%22I-don't-math%22-edition";
  private static final String SOURCE_URL       = "https://github.com/entangledloops/heuristicSearch/tree/master";
  private static final String HOMEPAGE_URL     = "http://www.entangledloops.com";
  private static final String DEFAULT_HOST     = "semiprime.servebeer.com";
  private static final String OS               = System.getProperty("os.name");

  //////////////////////////////////////////////////////////////////////////////
  // user prefs

  private Preferences prefs;

  private static final String WIDTH_NAME            = "width";
  private static final String HEIGHT_NAME           = "height";
  private static final String PROCESSORS_NAME       = "processors";
  private static final String CAP_NAME              = "name";
  private static final String MEMORY_NAME           = "memory";
  private static final String IDLE_NAME             = "idle";
  private static final String BACKGROUND_NAME       = "background";
  private static final String AUTOSTART_NAME        = "start search immediately";
  private static final String SEMIPRIME_NAME        = "semiprime";
  private static final String SAFETY_CONSCIOUS_NAME = "safety conscious";
  private static final String CPU_CONSCIOUS_NAME    = "cpu conscious";
  private static final String MEMORY_CONSCIOUS_NAME = "memory conscious";
  private static final String PRINT_ALL_NODES_NAME  = "print all nodes";
  private static final String WRITE_CSV_NAME        = "write nodes csv";

  private static final int TAB_CONNECT  = 0;
  private static final int TAB_SEARCH   = 1;
  private static final int TAB_CPU      = 2;
  private static final int TAB_SETTINGS = 3;
  private static final int HISTORY_ROWS = 5;
  private static final int HISTORY_COLS = 20;

  private static final int DEFAULT_PROCESSORS = Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_WIDTH      = 800;
  private static final int DEFAULT_HEIGHT     = 600;
  private static final int DEFAULT_CAP        = 100;
  private static final int DEFAULT_MEMORY     = 100;
  private static final int DEFAULT_IDLE       = 5;
  private static final int DEFAULT_PORT       = 12288;

  private static final boolean DEFAULT_WORK_ALWAYS      = false;
  private static final boolean DEFAULT_AUTOSTART        = false;
  private static final boolean DEFAULT_SAFETY_CONSCIOUS = Solver.safetyConscious();
  private static final boolean DEFAULT_CPU_CONSCIOUS    = Solver.cpuConscious();
  private static final boolean DEFAULT_MEMORY_CONSCIOUS = Solver.memoryConscious();
  private static final boolean DEFAULT_PRINT_ALL_NODES  = Solver.printAllNodes();
  private static final boolean DEFAULT_WRITE_CSV        = Solver.writeCsv();

  //////////////////////////////////////////////////////////////////////////////
  // gui

  private static final int H_GAP = 10;
  private static final int V_GAP = 10;

  // global
  private JTabbedPane         pneMain;
  private ImageIcon           icnNode, icnNodeSmall, icnCpu, icnNet, icnSettings;
  private SystemTray          systemTray;
  private TrayIcon            trayIcon;

  // connect tab
  private JTextField          txtUsername;
  private JTextField          txtEmail;
  private JTextField          txtAddress;
  private JTextArea           txtHistory;
  private JFormattedTextField txtPort;
  private JButton             btnConnect;
  private JButton             btnUpdate;

  // cpu tab
  private JSlider             sldProcessors, sldCap, sldMemory, sldIdle;
  private JCheckBox           chkBackground;
  private JCheckBox           chkAutoStart;

  // node tab
  private JCheckBox chkSafetyConscious;
  private JCheckBox chkCpuConscious;
  private JCheckBox chkMemoryConscious;
  private JCheckBox chkPrintAllNodes;
  private JCheckBox chkWriteCsv;
  private JTextArea txtSemiprime;
  private JButton   btnLocalSearch;

  //////////////////////////////////////////////////////////////////////////////
  // state

  private final AtomicReference<Thread> solver          = new AtomicReference<>();
  private final AtomicReference<Client> client          = new AtomicReference<>(null);
  private final AtomicBoolean           isConnecting    = new AtomicBoolean(false);
  private final AtomicBoolean           isUpdatePending = new AtomicBoolean(false);

  //////////////////////////////////////////////////////////////////////////////
  //
  // ClientGui
  //
  //////////////////////////////////////////////////////////////////////////////

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

  private void resetFrame()
  {
    setTitle(DEFAULT_TITLE);
    setState(Frame.NORMAL);
    setUndecorated(false);
    setResizable(true);
    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    setLocationRelativeTo(getRootPane());
  }

  private void exit()
  {
    saveSettings();
    sendWork();

    final Client connection = client.getAndSet(null);
    if (null != connection && connection.connected()) connection.close();
    if (null != systemTray && null != trayIcon) systemTray.remove(trayIcon);

    System.exit(0);
  }

  // re-initializes the window's components
  private boolean create()
  {
    ////////////////////////////////////////////////////////////////////////////
    // attempt to locate stored user settings for loading at end of init

    prefs = Preferences.userNodeForPackage(getClass());

    ////////////////////////////////////////////////////////////////////////////
    // connect tab

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
      final Runnable append = () ->
      {
        txtHistory.append(s + "\n");
        txtHistory.setCaretPosition(txtHistory.getText().length()-1);
      };
      try
      {
        if (SwingUtilities.isEventDispatchThread()) SwingUtilities.invokeLater(append);
        else append.run();
      }
      catch (Throwable ignored) {} // don't care what went wrong with gui update, it's been logged anyway
    });

    Log.d("\"Thank you\" raised to the 101st power for helping my semiprime research!");
    Log.d("If you're computer cracks a target number, you will be credited in the publication (assuming you provided an email I can reach you at).");
    Log.d("If you're interested in learning exactly what this software does and why, checkout the \"About\" menu.\n");

    final JScrollPane scrollPaneHistory = new JScrollPane(txtHistory);
    txtHistory.setHighlighter(new DefaultHighlighter());
    scrollPaneHistory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPaneHistory.setVisible(true);

    // username
    final JLabel lblUsername = new JLabel("Optional username:");
    lblUsername.setHorizontalAlignment(SwingConstants.CENTER);
    txtUsername = new JTextField(System.getProperty("user.name"));
    txtUsername.setHorizontalAlignment(SwingConstants.CENTER);

    // email
    final JLabel lblEmail = new JLabel("Optional email (in case you crack a number\u2014will never share):");
    lblEmail.setHorizontalAlignment(SwingConstants.CENTER);
    txtEmail = new JTextField(DEFAULT_EMAIL);
    txtEmail.setHorizontalAlignment(SwingConstants.CENTER);

    // host address label and text box
    final JLabel lblAddress = new JLabel("Server address:");
    lblAddress.setHorizontalAlignment(SwingConstants.CENTER);
    txtAddress = new JTextField(DEFAULT_HOST);
    txtAddress.setHorizontalAlignment(SwingConstants.CENTER);
    txtAddress.setColumns(DEFAULT_HOST.length());

    // port box and restrict to numbers
    final JLabel lblPort = new JLabel("Server port:");
    lblPort.setHorizontalAlignment(SwingConstants.CENTER);
    final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
    final DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
    decimalFormat.setGroupingUsed(false);
    txtPort = new JFormattedTextField(decimalFormat);
    txtPort.setHorizontalAlignment(SwingConstants.CENTER);
    txtPort.setColumns(5);
    txtPort.setText(DEFAULT_PORT+"");

    final JLabel lblConnectNow = new JLabel("Click update if you change your username or email after connecting:");
    lblConnectNow.setHorizontalAlignment(SwingConstants.CENTER);

    btnConnect = new JButton("Connect Now");
    btnConnect.setHorizontalAlignment(SwingConstants.CENTER);
    btnConnect.setFocusPainted(false);
    btnConnect.addActionListener(e ->
    {
      if (isConnecting.compareAndSet(false, true)) connect();
    });

    btnUpdate = new JButton("Update");
    btnUpdate.setHorizontalAlignment(SwingConstants.CENTER);
    btnUpdate.setFocusPainted(false);
    btnUpdate.setEnabled(false);
    btnUpdate.addActionListener(e -> sendSettings());

    final JPanel pnlConnectBtn = new JPanel(new GridLayout(1,2));
    pnlConnectBtn.add(btnConnect);
    pnlConnectBtn.add(btnUpdate);

    // add the components to the left-side connect region
    final JPanel pnlConnect = new JPanel(new GridLayout(10, 1));
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

    // organize them and add them to the panel
    final JPanel pnlNet = new JPanel(new GridLayout(2, 1));
    pnlNet.add(scrollPaneHistory);
    pnlNet.add(pnlConnect);

    ////////////////////////////////////////////////////////////////////////////
    // setup the icons and menus
    try
    {
      final boolean jar = Utils.jar();
      icnNodeSmall = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_NODE_SMALL))) : new ImageIcon(Utils.getResource(ICON_NODE_SMALL));
      icnNode = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_NODE))) : new ImageIcon(Utils.getResource(ICON_NODE));
      icnCpu = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_CPU))) : new ImageIcon(Utils.getResource(ICON_CPU));
      icnNet = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_NET))) : new ImageIcon(Utils.getResource(ICON_NET));
      icnSettings = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_SETTINGS))) : new ImageIcon(Utils.getResource(ICON_SETTINGS));
      setIconImage(icnNode.getImage());
    }
    catch (Throwable t) { Log.e(t); }

    final JMenuBar mnuBar = new JMenuBar();

    final JMenu mnuFile = new JMenu("File");
    mnuBar.add(mnuFile);

    final JMenuItem mnuSaveSettings = new JMenuItem("Save Settings");
    mnuSaveSettings.addActionListener(l -> saveSettings());
    mnuFile.add(mnuSaveSettings);

    final JMenuItem mnuLoadSettings = new JMenuItem("Load Settings");
    mnuLoadSettings.addActionListener(l -> loadSettings());
    mnuFile.add(mnuLoadSettings);

    mnuFile.addSeparator();

    final JMenuItem mnuSendWork = new JMenuItem("Send All Completed Work");
    mnuSendWork.addActionListener(l -> sendWork());
    mnuFile.add(mnuSendWork);

    final JMenuItem mnuRecvWork = new JMenuItem("Request a New Node");
    mnuRecvWork.addActionListener(l -> recvWork());
    mnuFile.add(mnuRecvWork);

    mnuFile.addSeparator();

    final JMenuItem mnuQuit = new JMenuItem("Save & Quit");
    mnuQuit.addActionListener(l -> exit());
    mnuFile.add(mnuQuit);

    try
    {
      final JMenu mnuAbout = new JMenu("About");

      final URI aboutURI = new URI(ABOUT_URL);
      final JMenuItem mnuSPF = new JMenuItem("What is Semiprime Factorization?");
      mnuSPF.addActionListener(l ->
      {
        try { java.awt.Desktop.getDesktop().browse(aboutURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuSPF);

      final URI noMathURI = new URI(NO_MATH_URL);
      final JMenuItem mnuNoMath = new JMenuItem("Explain it again, but like I don't know any math.");
      mnuNoMath.addActionListener(l ->
      {
        try { java.awt.Desktop.getDesktop().browse(noMathURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuNoMath);

      mnuAbout.addSeparator();

      final URI downloadURI = new URI(SOURCE_URL);
      final JMenuItem mnuDownload = new JMenuItem("Download Latest Client");
      mnuDownload.addActionListener(l ->
      {
        try { java.awt.Desktop.getDesktop().browse(downloadURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuDownload);

      final URI sourceURI = new URI(SOURCE_URL);
      final JMenuItem mnuSource = new JMenuItem("Source Code");
      mnuSource.addActionListener(l ->
      {
        try { java.awt.Desktop.getDesktop().browse(sourceURI); }
        catch (Throwable t) { Log.e(t); }
      });
      mnuAbout.add(mnuSource);

      mnuAbout.addSeparator();

      final URI homepageURI = new URI(HOMEPAGE_URL);
      final JMenuItem mnuHomepage = new JMenuItem("My Homepage");
      mnuHomepage.addActionListener(l ->
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
    // search tab

    chkSafetyConscious = new JCheckBox(SAFETY_CONSCIOUS_NAME, DEFAULT_SAFETY_CONSCIOUS);
    chkSafetyConscious.addActionListener((e) -> Solver.safetyConscious(chkSafetyConscious.isSelected()));
    chkSafetyConscious.setHorizontalAlignment(SwingConstants.CENTER);
    chkSafetyConscious.setFocusPainted(false);

    chkCpuConscious = new JCheckBox(CPU_CONSCIOUS_NAME, DEFAULT_CPU_CONSCIOUS);
    chkCpuConscious.addActionListener((e) -> Solver.cpuConscious(chkCpuConscious.isSelected()));
    chkCpuConscious.setHorizontalAlignment(SwingConstants.CENTER);
    chkCpuConscious.setFocusPainted(false);

    chkMemoryConscious = new JCheckBox(MEMORY_CONSCIOUS_NAME, DEFAULT_MEMORY_CONSCIOUS);
    chkMemoryConscious.addActionListener((e) -> Solver.memoryConscious(chkMemoryConscious.isSelected()));
    chkMemoryConscious.setHorizontalAlignment(SwingConstants.CENTER);
    chkMemoryConscious.setFocusPainted(false);

    chkPrintAllNodes = new JCheckBox(PRINT_ALL_NODES_NAME, DEFAULT_PRINT_ALL_NODES);
    chkPrintAllNodes.addActionListener((e) -> Solver.printAllNodes(chkPrintAllNodes.isSelected()));
    chkPrintAllNodes.setHorizontalAlignment(SwingConstants.CENTER);
    chkPrintAllNodes.setFocusPainted(false);

    chkWriteCsv = new JCheckBox(WRITE_CSV_NAME, DEFAULT_WRITE_CSV);
    chkWriteCsv.addActionListener((e) -> Solver.writeCsv(chkWriteCsv.isSelected()));
    chkWriteCsv.setHorizontalAlignment(SwingConstants.CENTER);
    chkWriteCsv.setFocusPainted(false);

    txtSemiprime = new JTextArea(HISTORY_ROWS, HISTORY_COLS);
    txtSemiprime.setHighlighter(new DefaultHighlighter());
    txtSemiprime.setText("323");

    final JScrollPane scrollPaneSemiprime = new JScrollPane(txtSemiprime);
    scrollPaneSemiprime.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPaneSemiprime.setVisible(true);

    final JLabel lblSemiprimeBase = new JLabel("Semiprime Base");
    lblSemiprimeBase.setHorizontalAlignment(SwingConstants.CENTER);
    final JLabel lblInternalBase = new JLabel("Internal Base");
    lblInternalBase.setHorizontalAlignment(SwingConstants.CENTER);
    final JTextField txtSemiprimeBase = new JTextField("10");
    txtSemiprimeBase.setHorizontalAlignment(SwingConstants.CENTER);
    final JTextField txtInternalBase = new JTextField("2");
    txtInternalBase.setHorizontalAlignment(SwingConstants.CENTER);

    btnLocalSearch = new JButton("Start Local Search");
    btnLocalSearch.addActionListener(e ->
    {
      // interrupt any previous solver and wait for termination
      final Thread prev = solverThread(null);
      if (null != prev) { try { Solver.interrupt(); prev.join(); } catch (Throwable ignored) {} }

      // grab the entered search options
      int spBase = 10;
      try { spBase = Integer.parseInt(txtSemiprimeBase.getText().trim()); }
      catch (Throwable t) { Log.e("provided semiprime base was invalid, defaulting to 10"); txtSemiprimeBase.setText("10"); }
      finally { if (spBase < 2) { Log.e("semiprime base cannot be < 2, defaulting to 10"); txtSemiprimeBase.setText("10"); } }

      int internalBase = 10;
      try { internalBase = Integer.parseInt(txtInternalBase.getText().trim()); }
      catch (Throwable t) { Log.e("provided internal base was invalid, defaulting to 10"); txtInternalBase.setText("10"); }
      finally { if (internalBase < 2) { Log.e("internal base cannot be < 2, defaulting to 10"); txtInternalBase.setText("10"); } }

      // reset the solver for a new search
      Solver.reset();

      // create a new solver based upon user request
      try
      {
        final Solver solver = Solver.newInstance(txtSemiprime.getText().replace(" ", "").replace("\n","").replace("\t","").trim(), spBase, internalBase);
        if (null == solver) return;

        // set the callback to trigger on completion
        Solver.callback(n -> {
            if (null == n) { Log.d("no factors were found, are you sure the input is semiprime?"); return; }
            Log.d("\nfactors found:\n" +
                "\nsp:\n\t" + solver.toString(10) + " (" + solver.toString(2) + ")" +
                "\np1:\n\t" + n.p(0, 10) + " (" + n.p(0, 2) + ")" +
                "\np2:\n\t" + n.p(1, 10) + " (" + n.p(1, 2) + ")");
        });

        // set the new solver thread
        solverThread(new Thread(solver));

        // finally, if all went well launch the search
        final Thread solverThread = solverThread();
        if (null != solverThread)
        {
          solverThread.start();
          pneMain.setSelectedIndex(TAB_CONNECT);
          updateSettings();
        }
      }
      catch (Throwable t) { Log.e(t); }
    });

    final JLabel lblSemiprime = new JLabel("Semiprime Target");
    lblSemiprime.setIcon(icnNodeSmall);
    lblSemiprime.setHorizontalAlignment(SwingConstants.CENTER);

    final JPanel pnlSearchOptions = new JPanel(new GridLayout(2, 3));
    pnlSearchOptions.add(chkSafetyConscious);
    pnlSearchOptions.add(chkCpuConscious);
    pnlSearchOptions.add(chkMemoryConscious);
    pnlSearchOptions.add(chkPrintAllNodes);
    pnlSearchOptions.add(chkWriteCsv);

    final JPanel pnlSemiprime = new JPanel(new GridLayout(2,1));
    pnlSemiprime.add(lblSemiprime);
    pnlSemiprime.add(scrollPaneSemiprime);

    final JPanel pnlSemiprimeOptions = new JPanel(new GridLayout(2,2));
    pnlSemiprimeOptions.add(lblSemiprimeBase);
    pnlSemiprimeOptions.add(txtSemiprimeBase);
    pnlSemiprimeOptions.add(lblInternalBase);
    pnlSemiprimeOptions.add(txtInternalBase);

    final JPanel pnlLocalSearch = new JPanel(new GridLayout(2, 1));
    pnlLocalSearch.add(pnlSemiprimeOptions);
    pnlLocalSearch.add(btnLocalSearch);

    final JPanel pnlSearch = new JPanel(new GridLayout(3, 1));
    pnlSearch.add(pnlSearchOptions);
    pnlSearch.add(pnlSemiprime);
    pnlSearch.add(pnlLocalSearch);

    ////////////////////////////////////////////////////////////////////////////
    // cpu tab

    // setup the memory/ processing limit sliders:
    final JLabel lblProcessors = new JLabel("Processors to use");
    lblProcessors.setHorizontalAlignment(SwingConstants.CENTER);
    sldProcessors = new JSlider(1, processors, prefs.getInt(PROCESSORS_NAME, DEFAULT_PROCESSORS));
    sldProcessors.setMajorTickSpacing(1);
    sldProcessors.setSnapToTicks(true);
    sldProcessors.setPaintTicks(true);
    sldProcessors.setPaintLabels(true);
    sldProcessors.addChangeListener(c ->
    {
      if (sldProcessors.getValueIsAdjusting()) return;
      int val = sldProcessors.getValue();
      Solver.processors(val);
      Log.d("processor cap adjusted: " + val);
    });


    final JLabel lblCap = new JLabel("Per-processor usage (%)");
    lblCap.setHorizontalAlignment(SwingConstants.CENTER);
    sldCap = new JSlider(0, 100, prefs.getInt(CAP_NAME, DEFAULT_CAP));
    sldCap.setMajorTickSpacing(25);
    sldCap.setMinorTickSpacing(5);
    sldCap.setSnapToTicks(true);
    sldCap.setPaintLabels(true);
    sldCap.setPaintTicks(true);
    sldCap.addChangeListener(c ->
    {
      if (sldCap.getValueIsAdjusting()) return;
      int val = sldCap.getValue();
      Log.d("CPU cap adjusted: " + val + "%");
    });

    final JLabel lblMemory = new JLabel("Memory usage (%)");
    lblMemory.setHorizontalAlignment(SwingConstants.CENTER);
    sldMemory = new JSlider(0, 100, prefs.getInt(MEMORY_NAME, DEFAULT_MEMORY));
    sldMemory.setMajorTickSpacing(25);
    sldMemory.setMinorTickSpacing(5);
    sldMemory.setSnapToTicks(true);
    sldMemory.setPaintLabels(true);
    sldMemory.setPaintTicks(true);
    sldMemory.addChangeListener(c ->
    {
      if (sldMemory.getValueIsAdjusting()) return;
      int val = sldMemory.getValue();
      Log.d("memory cap adjusted: " + val + "%");
    });

    final JLabel lblIdle = new JLabel("Idle time until work begins (min)");
    lblIdle.setHorizontalAlignment(SwingConstants.CENTER);
    sldIdle = new JSlider(0, 30, prefs.getInt(IDLE_NAME, DEFAULT_IDLE));
    sldIdle.setMajorTickSpacing(5);
    sldIdle.setMinorTickSpacing(1);
    sldIdle.setSnapToTicks(true);
    sldIdle.setPaintLabels(true);
    sldIdle.setPaintTicks(true);
    sldIdle.addChangeListener(c ->
    {
      if (sldIdle.getValueIsAdjusting()) return;
      int val = sldIdle.getValue();
      Log.d("time until work begins adjusted: " + val + " minutes");
    });

    final JButton btnResetCpu = new JButton("Reset CPU Settings to Defaults");
    btnResetCpu.setHorizontalAlignment(SwingConstants.CENTER);
    btnResetCpu.setFocusPainted(false);
    btnResetCpu.addActionListener(l ->
    {
      final int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to reset all CPU settings to defaults?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
      if (JOptionPane.YES_OPTION == result) resetCpuSettings();
    });

    // setup the left-side:
    final JPanel pnlCpuLeft = new JPanel(new GridLayout(7, 1, H_GAP, V_GAP));
    pnlCpuLeft.add(lblProcessors);
    pnlCpuLeft.add(sldProcessors);
    pnlCpuLeft.add(lblCap);
    pnlCpuLeft.add(sldCap);
    pnlCpuLeft.add(new JLabel(""));
    pnlCpuLeft.add(new JLabel(""));
    pnlCpuLeft.add(btnResetCpu);

    // setup the right-side:
    final JPanel pnlCpuRight = new JPanel(new GridLayout(7, 1, H_GAP, V_GAP));

    // setup connect button and "always work" checkbox
    chkBackground = new JCheckBox("work in background", prefs.getBoolean(BACKGROUND_NAME, DEFAULT_WORK_ALWAYS));
    chkBackground.setHorizontalAlignment(SwingConstants.CENTER);
    chkBackground.setFocusPainted(false);
    chkBackground.addActionListener(l -> { Solver.background(chkBackground.isSelected()); Log.d("background: " + (Solver.background() ? "yes" : "no")); });

    // auto start with system?
    chkAutoStart = new JCheckBox("auto-start with system", prefs.getBoolean(AUTOSTART_NAME, DEFAULT_AUTOSTART));
    chkAutoStart.setHorizontalAlignment(SwingConstants.CENTER);
    chkAutoStart.setFocusPainted(false);
    chkAutoStart.addActionListener(l -> Log.d("autostart: " + (chkAutoStart.isSelected() ? "yes" : "no")));

    final JPanel pnlChkBoxes = new JPanel(new GridLayout(2, 1, H_GAP, V_GAP));
    pnlChkBoxes.add(chkBackground);
    pnlChkBoxes.add(chkAutoStart);

    pnlCpuRight.add(lblMemory);
    pnlCpuRight.add(sldMemory);
    pnlCpuRight.add(lblIdle);
    pnlCpuRight.add(sldIdle);
    pnlCpuRight.add(new JLabel(""));
    pnlCpuRight.add(new JLabel(""));
    pnlCpuRight.add(pnlChkBoxes);

    // create the full CPU panel
    final JPanel pnlCpu = new JPanel(new GridLayout(1, 2, H_GAP, V_GAP));
    pnlCpu.add(pnlCpuLeft);
    pnlCpu.add(pnlCpuRight);

    ////////////////////////////////////////////////////////////////////////////
    // settings tab

    final JPanel pnlSettings = new JPanel();

    ////////////////////////////////////////////////////////////////////////////
    // add tabs to frame

    // create the tabbed panes
    pneMain = new JTabbedPane();
    pneMain.setFocusable(false);
    pneMain.setBorder(null);
    pneMain.addTab("", icnNet, pnlNet);
    pneMain.addTab("", icnNode, pnlSearch);
    pneMain.addTab("", icnCpu, pnlCpu);
    pneMain.addTab("", icnSettings, pnlSettings);

    // add the panel to the frame and show everything
    getContentPane().add(pneMain);
    resetFrame();

    ////////////////////////////////////////////////////////////////////////////
    // setup the system tray, if supported

    boolean useTray = SystemTray.isSupported();
    if (useTray)
    {
      setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

      systemTray = SystemTray.getSystemTray();
      trayIcon = new TrayIcon(icnNodeSmall.getImage(), "Semiprime Factorization v" + VERSION);
      final PopupMenu popup = new PopupMenu();

      final MenuItem show = new MenuItem("Hide App");
      final Runnable trayVisibleToggle = () ->
      {
        final boolean visible = !isVisible();
        setVisible(visible);
        show.setLabel(visible ? "Hide App" : "Show App");
      };
      show.addActionListener(l -> trayVisibleToggle.run());

      final MenuItem pause = new MenuItem("Pause");
      final MenuItem resume = new MenuItem("Resume");
      resume.setEnabled(false);
      pause.addActionListener(l ->
      {
        pause.setEnabled(false);
        pause();
        trayIcon.displayMessage("Paused", "All work has been paused.", TrayIcon.MessageType.INFO);
        resume.setEnabled(true);
      });
      resume.addActionListener(l ->
      {
        resume.setEnabled(false);
        resume();
        trayIcon.displayMessage("Resumed", "All work has been resumed.", TrayIcon.MessageType.INFO);
        pause.setEnabled(true);
      });

      final MenuItem quit = new MenuItem("Save & Quit");
      quit.addActionListener(l -> exit());

      trayIcon.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e)
        {
          final int btn = e.getButton(); final boolean osx = OS.contains("OS X");
          if ( (btn == MouseEvent.BUTTON1 && !osx) || (btn != MouseEvent.BUTTON1 && osx) ) trayVisibleToggle.run();
        }
      });

      popup.add(show);
      popup.addSeparator();
      popup.add(pause);
      popup.add(resume);
      popup.addSeparator();
      popup.add(quit);

      trayIcon.setPopupMenu(popup);

      try { systemTray.add(trayIcon); }
      catch (Throwable t) { Log.e("couldn't create a tray icon, will exit on window close instead"); useTray = false; }
    }

    // on close, kill the server connection if there is no tray icon in use
    if (!useTray) addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { exit(); } });

    // attempt to load stored settings
    loadSettings();

    // collect garbage and report memory info
    runtime.gc();
    final double freeMemory = (double)runtime.freeMemory() / toGb;
    final double totalMemory = (double)runtime.totalMemory() / toGb;
    final double maxMemory =  (double)runtime.maxMemory() / toGb;
    final DecimalFormat formatter = new DecimalFormat("#.##");

    // report discovered system stats
    Log.d(
        "operating system: " + OS + ", version " + System.getProperty("os.version") + "\n" +
        "current java version: " + version + ", required: 1.8+\n" +
        "note: all memory values reported are relative to the JVM, and were reported immediately after invoking the GC\n" +
        "free memory: ~" + formatter.format(freeMemory) + " (Gb)\n" +
        "total memory: ~" + formatter.format(totalMemory) + " (Gb)\n" +
        "max memory: ~" + formatter.format(maxMemory) + " (Gb)\n" +
        "free memory / total memory: " + formatter.format(100.0*(freeMemory/totalMemory)) + "%\n" +
        "total memory / max memory: " + formatter.format(100.0*(totalMemory/maxMemory)) + "%\n" +
        "always work: " + chkBackground.isSelected() + "\n" +
        "autostart: " + chkAutoStart.isSelected() + "\n" +
        "available processors: " + processors
    );

    return true;
  }

  private Thread solverThread(Thread solver) { return this.solver.getAndSet(solver); }
  private Thread solverThread() { return solver.get(); }

  private Client client(Client client) { this.client.set(client); return this.client.get(); }
  private Client client() { return client.get(); }

  private void updateNetworkComponents() { updateNetworkComponents(null); }
  private void updateNetworkComponents(Packet p)
  {
    if (null != p) return;
    final Client c = client.get();
    if (isConnecting.get())
    {
      btnUpdate.setEnabled(false);
      btnConnect.setEnabled(false);
      btnConnect.setText("Connecting...");
    }
    else if (null == c || !c.connected())
    {
      btnUpdate.setEnabled(false);
      btnConnect.setEnabled(true);
      btnConnect.setText("Connect Now");
    }
    else
    {
      btnUpdate.setEnabled( !isUpdatePending.get() );
      btnConnect.setEnabled(true);
      btnConnect.setText("Disconnect");
    }
  }

  private boolean autostart() { return chkAutoStart.isSelected(); }

  private void pause()
  {
    Log.d("pausing all work...");

    Log.d("all work paused");
  }

  private void resume()
  {
    Log.d("resuming all work...");

    Log.d("all work resumed");
  }

  private void connect()
  {
    new Thread(() ->
    {
      try
      {
        // close any old connection
        final Client prev = client.getAndSet(null);
        if (null != prev && prev.connected())
        {
          prev.close();
          return;
        }

        isConnecting.set(true);
        updateNetworkComponents();

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

        final Client c = new Client(address, port, this::updateNetworkComponents);
        client.set(c);

        sendSettings();
      }
      catch (NumberFormatException e)
      {
        Log.e("connect failure: " + e.getMessage());
      }
      catch (NullPointerException e)
      {
        JOptionPane.showMessageDialog(this, "Couldn't locate the semiprime server.\n" +
            "Will keep retrying every few minutes, and the prime search will begin independently in the meantime.\n\n" +
            "If you're feeling lucky or want to try a different server, you can retry connecting anytime.");
      }
      finally
      {
        isConnecting.set(false);
        updateNetworkComponents();
      }
    }).start();
  }

  private void sendSettings()
  {
    Log.d("sending user settings to the server...");

    // ensure a client exists
    final Client client = client();
    if (null == client || !client.connected()) { Log.e("you need to be connected before updating"); return; }

    // ensure we aren't overlapping w/another thread
    if (!isUpdatePending.compareAndSet(false, true)) { Log.e("already working on an update, hang tight..."); return; }
    btnUpdate.setEnabled(false);
    txtUsername.setEnabled(false);
    txtEmail.setEnabled(false);

    // grab the info from the connect boxes and clean it up
    String username = txtUsername.getText().trim();
    if ("".equals(username)) username = System.getProperty("user.name");
    txtUsername.setText(username);

    String email = txtEmail.getText().trim();
    if ("".equals(email)) email = DEFAULT_EMAIL;
    txtEmail.setText(email);

    // attempt to send each piece of client info to the server
    client.setUsername(username); if (!client.sendPacket(Packet.USERNAME_UPDATE)) { Log.e("failed to send username, try reconnecting"); return; }
    client.setEmail(email); if (!client.sendPacket(Packet.EMAIL_UPDATE)) { Log.e("failed to send email, try reconnecting"); return; }

    // if all went all, update our state
    txtEmail.setEnabled(true);
    txtUsername.setEnabled(true);
    btnUpdate.setEnabled(true);
    isUpdatePending.set(false);

    Log.d("the server has acknowledged your settings");
  }

  private void saveCpuSettings()
  {
    prefs.putInt(PROCESSORS_NAME, sldProcessors.getValue());
    prefs.putInt(CAP_NAME, sldCap.getValue());
    prefs.putInt(MEMORY_NAME, sldMemory.getValue());
    prefs.putInt(IDLE_NAME, sldIdle.getValue());

    prefs.putBoolean(BACKGROUND_NAME, chkBackground.isSelected());
    prefs.putBoolean(AUTOSTART_NAME, chkAutoStart.isSelected());

    Log.d("cpu settings saved");
  }

  private void saveSearchSettings()
  {
    prefs.putByteArray(SEMIPRIME_NAME, txtSemiprime.getText().trim().getBytes());

    prefs.putBoolean(SAFETY_CONSCIOUS_NAME, chkSafetyConscious.isSelected());
    prefs.putBoolean(CPU_CONSCIOUS_NAME, chkCpuConscious.isSelected());
    prefs.putBoolean(MEMORY_CONSCIOUS_NAME, chkMemoryConscious.isSelected());
    prefs.putBoolean(PRINT_ALL_NODES_NAME, chkPrintAllNodes.isSelected());
    prefs.putBoolean(WRITE_CSV_NAME, chkWriteCsv.isSelected());
  }


  private void saveSettings()
  {
    Log.d("saving settings...");

    try
    {
      saveCpuSettings();
      saveSearchSettings();

      prefs.flush();
    }
    catch (Throwable t) { Log.e("failed to store preferences. make sure the app has write permissions"); return; }

    Log.d("all settings saved");
  }

  private void loadCpuSettings()
  {
    sldProcessors.setValue(prefs.getInt(PROCESSORS_NAME, DEFAULT_PROCESSORS));
    sldCap.setValue(prefs.getInt(CAP_NAME, DEFAULT_CAP));
    sldMemory.setValue(prefs.getInt(MEMORY_NAME, DEFAULT_MEMORY));
    sldIdle.setValue(prefs.getInt(IDLE_NAME, DEFAULT_IDLE));

    chkBackground.setSelected(prefs.getBoolean(BACKGROUND_NAME, DEFAULT_WORK_ALWAYS));
    chkAutoStart.setSelected(prefs.getBoolean(AUTOSTART_NAME, DEFAULT_AUTOSTART));

    Log.d("cpu settings loaded");
  }

  private void loadSearchSettings()
  {
    // parse semiprime stored bytes -> string
    final byte[] sp = prefs.getByteArray(SEMIPRIME_NAME, null);
    final String temp = null != sp ? Arrays.toString(sp) : "[]";
    final String[] parts = temp.substring(1, temp.length()-1).split(",");
    final byte[] semiprime = new byte[parts.length];
    int i = -1; for (String s : parts) semiprime[++i] = Byte.parseByte(s.trim());
    txtSemiprime.setText(new String(semiprime));

    chkSafetyConscious.setSelected(prefs.getBoolean(SAFETY_CONSCIOUS_NAME, DEFAULT_SAFETY_CONSCIOUS));
    chkCpuConscious.setSelected(prefs.getBoolean(CPU_CONSCIOUS_NAME, DEFAULT_CPU_CONSCIOUS));
    chkMemoryConscious.setSelected(prefs.getBoolean(MEMORY_CONSCIOUS_NAME, DEFAULT_MEMORY_CONSCIOUS));
    chkPrintAllNodes.setSelected(prefs.getBoolean(PRINT_ALL_NODES_NAME, DEFAULT_PRINT_ALL_NODES));
    chkWriteCsv.setSelected(prefs.getBoolean(WRITE_CSV_NAME, DEFAULT_WRITE_CSV));

    Log.d("search settings loaded");
  }

  private void loadSettings()
  {
    Log.d("loading settings...");

    try
    {
      prefs = Preferences.userNodeForPackage(getClass());

      loadCpuSettings();
      loadSearchSettings();
    }
    catch (Throwable t) { Log.e("failed to load settings. make sure app has read permissions"); return; }

    Log.d("all settings loaded");
  }

  private void updateSearchSettings()
  {
    chkSafetyConscious.setSelected(Solver.safetyConscious());
    chkCpuConscious.setSelected(Solver.cpuConscious());
    chkMemoryConscious.setSelected(Solver.memoryConscious());
    chkPrintAllNodes.setSelected(Solver.printAllNodes());
    chkWriteCsv.setSelected(Solver.writeCsv());
  }

  private void updateSettings()
  {
    updateSearchSettings();
  }

  private void resetCpuSettings()
  {
    sldProcessors.setValue(DEFAULT_PROCESSORS);
    sldCap.setValue(DEFAULT_CAP);
    sldMemory.setValue(DEFAULT_MEMORY);
    sldIdle.setValue(DEFAULT_IDLE);

    chkBackground.setSelected(DEFAULT_WORK_ALWAYS);
    chkAutoStart.setSelected(DEFAULT_AUTOSTART);

    Log.d("cpu settings reset");
  }

  private void resetSearchSettings()
  {
    txtSemiprime.setText("");

    chkSafetyConscious.setSelected(DEFAULT_SAFETY_CONSCIOUS);
    chkCpuConscious.setSelected(DEFAULT_CPU_CONSCIOUS);
    chkMemoryConscious.setSelected(DEFAULT_MEMORY_CONSCIOUS);
    chkPrintAllNodes.setSelected(DEFAULT_PRINT_ALL_NODES);
    chkWriteCsv.setSelected(DEFAULT_WRITE_CSV);

    Log.d("search settings reset");
  }

  private void resetSettings()
  {
    Log.d("resetting all settings to defaults...");

    resetCpuSettings();
    resetSearchSettings();

    Log.d("settings reset");
  }

  private void sendWork()
  {
    Log.d("sending all completed work to server...");

    Log.d("server successfully received all completed work");
  }

  private void recvWork()
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
