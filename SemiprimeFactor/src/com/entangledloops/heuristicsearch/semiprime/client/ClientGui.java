package com.entangledloops.heuristicsearch.semiprime.client;

import com.entangledloops.heuristicsearch.semiprime.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigInteger;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;

/**
 * @author Stephen Dunn
 * @since November 1, 2015
 */
public class ClientGui extends JFrame implements DocumentListener
{
  //////////////////////////////////////////////////////////////////////////////
  // benchmarks

  //
  // RSA-100 = 37975227936943673922808872755445627854565536638199
  //         × 40094690950920881030683735292761468389214899724061
  //
  private static final String RSA_100 = "15226050279225333605356183781326374297180681" +
      "14961380688657908494580122963258\n952897654000350692006139";

  //
  // RSA-220 = unsolved
  //
  private static final String RSA_220 = "22601385262034057849416540486101975135080389" +
      "15719776718321197768109445641817\n96667660859312130658257725063156288667697044" +
      "80700018111497118630021124879281\n99487482066070131066586646083327982803560379" +
      "205391980139946496955261";

  //
  // RSA-300 = unsolved
  //
  private static final String RSA_300 = "27693155678034421390286890616472330922376083" +
      "63983953254005036722809375824714\n94739461900602187562551243171865731050750745" +
      "46238828817121274630072161346956\n43967418363899790869043044724760018390159830" +
      "33451909174663464663867829125664\n45989557515717881690022879271126747195835757" +
      "4416714366499722090015674047";

  //
  // RSA-2048 = unsolved
  //
  private static final String RSA_2048 = "2519590847565789349402718324004839857142928" +
      "212620403202777713783604366202070\n7595556264018525880784406918290641249515082" +
      "189298559149176184502808489120072\n8449926873928072877767359714183472702618963" +
      "750149718246911650776133798590957\n0009733045974880842840179742910064245869181" +
      "719511874612151517265463228221686\n9987549182422433637259085141865462043576798" +
      "423387184774447920739934236584823\n8242811981638150106748104516603773060562016" +
      "196762561338441436038339044149526\n3443219011465754445417842402092461651572335" +
      "077870774981712577246796292638635\n6373289912154831438167899885040445364023527" +
      "381951378636564391212010397122822\n120720357";

  //////////////////////////////////////////////////////////////////////////////
  // globals

  private static final String VERSION          = "0.4.3a";
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
  private static final String IDLE_MINUTES_NAME      = "idle minutes";
  private static final String AUTOSTART_NAME         = "start search immediately";
  private static final String SEMIPRIME_NAME         = "semiprime";
  private static final String PERIODIC_STATS_NAME    = "periodic stats";
  private static final String DETAILED_STATS_NAME    = "detailed stats";
  private static final String FAVOR_PERFORMANCE_NAME = "favor performance";
  private static final String RESTRICT_DISK_NAME     = "restrict disk";
  private static final String RESTRICT_NETWORK_NAME  = "restrict network";
  private static final String BACKGROUND_NAME        = "background";
  private static final String COMPRESS_MEMORY_NAME   = "compress memory";
  private static final String PRINT_ALL_NODES_NAME   = "print all nodes";
  private static final String WRITE_CSV_NAME         = "write nodes csv";
  private static final String PROCESSORS_NAME        = "processors";
  private static final String PROCESSOR_CAP_NAME     = "processor cap";
  private static final String MEMORY_CAP_NAME        = "memory cap";
  private static final String SEMIPRIME_BASE_NAME    = "semiprime base";
  private static final String INTERNAL_BASE_NAME     = "internal base";
  private static final String P1_LEN_NAME            = "p1 len";
  private static final String P2_LEN_NAME            = "p2 len";

  private static final int TAB_CONNECT  = 0;
  private static final int TAB_SEARCH   = 1;
  private static final int TAB_CPU      = 2;
  private static final int TAB_MISC     = 3;
  private static final int HISTORY_ROWS = 5;
  private static final int HISTORY_COLS = 20;

  private static final int DEFAULT_SEMIPRIME_BASE = 10;
  private static final int DEFAULT_INTERNAL_BASE  = Solver.internalBase();
  private static final int DEFAULT_P1_LEN         = Solver.prime1Len();
  private static final int DEFAULT_P2_LEN         = Solver.prime2Len();
  private static final int DEFAULT_PROCESSORS     = Solver.processors();
  private static final int DEFAULT_PROCESSOR_CAP  = Solver.processorCap();
  private static final int DEFAULT_MEMORY_CAP     = Solver.memoryCap();
  private static final int DEFAULT_IDLE_MINUTES   = 5;
  private static final int DEFAULT_PORT           = 12288;
  private static final int DEFAULT_WIDTH          = 1024;
  private static final int DEFAULT_HEIGHT         = 768;

  private static final boolean DEFAULT_AUTOSTART         = false;
  private static final boolean DEFAULT_PERIODIC_STATS    = Solver.periodicStats();
  private static final boolean DEFAULT_DETAILED_STATS    = Solver.detailedStats();
  private static final boolean DEFAULT_FAVOR_PERFORMANCE = Solver.favorPerformance();
  private static final boolean DEFAULT_COMPRESS_MEMORY   = Solver.compressMemory();
  private static final boolean DEFAULT_RESTRICT_DISK     = Solver.restrictDisk();
  private static final boolean DEFAULT_RESTRICT_NETWORK  = Solver.restrictNetwork();
  private static final boolean DEFAULT_BACKGROUND        = Solver.background();
  private static final boolean DEFAULT_PRINT_ALL_NODES   = Solver.printAllNodes();
  private static final boolean DEFAULT_WRITE_CSV         = Solver.writeCsv();

  //////////////////////////////////////////////////////////////////////////////
  // gui

  private static final int H_GAP = 10;
  private static final int V_GAP = 10;

  // global
  private JTabbedPane pneMain;
  private ImageIcon   icnNode, icnNodeSmall, icnCpu, icnNet, icnMisc;
  private SystemTray systemTray;
  private TrayIcon   trayIcon;

  // connect tab
  private JTextField txtUsername;
  private JTextField txtEmail;
  private JTextField txtAddress;
  private JTextArea  txtHistory;
  private JTextField txtPort;
  private JButton    btnConnect;
  private JButton    btnUpdate;

  // search tab
  private JCheckBox  chkPeriodicStats;
  private JCheckBox  chkDetailedStats;
  private JCheckBox  chkFavorPerformance;
  private JCheckBox  chkCompressMemory;
  private JCheckBox  chkRestrictDisk;
  private JCheckBox  chkRestrictNetwork;
  private JCheckBox  chkBackground;
  private JCheckBox  chkPrintAllNodes;
  private JCheckBox  chkWriteCsv;
  private JTable     tblHeuristics;
  private JButton    btnSearch;
  private JLabel     lblSemiprime;
  private JTextArea  txtSemiprime;
  private JTextField txtSemiprimeBase, txtInternalBase, txtP1Len, txtP2Len;

  // cpu tab
  private JSlider sldProcessors, sldProcessorCap, sldMemoryCap, sldIdle;
  private JCheckBox chkAutoStart;

  //////////////////////////////////////////////////////////////////////////////
  // state

  private final AtomicReference<Thread> solver          = new AtomicReference<>();
  private final AtomicReference<Client> client          = new AtomicReference<>(null);
  private final AtomicBoolean           isConnecting    = new AtomicBoolean(false);
  private final AtomicBoolean           isUpdatePending = new AtomicBoolean(false);
  private final AtomicBoolean           isSearching     = new AtomicBoolean(false);

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

  private void exit() { exit(true); }
  private void exit(boolean save)
  {
    if (save) saveSettings();
    sendWork();

    final Client client = this.client.getAndSet(null);
    if (null != client && client.connected()) client.close();
    if (null != systemTray && null != trayIcon) systemTray.remove(trayIcon);

    System.exit(0);
  }

  /**
   * Generates a new window for the app that provides a gui to manage a local
   * search or contribute to an ongoing server-based search.
   *
   * @return true if everything went okay, false otherwise
   */
  private boolean create()
  {
    // the preferences object that will hold all user settings
    prefs = Preferences.userNodeForPackage(getClass());

    ////////////////////////////////////////////////////////////////////////////
    // icons

    try
    {
      final boolean jar = Utils.jar();
      icnNodeSmall = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_NODE_SMALL))) : new ImageIcon(Utils.getResource(ICON_NODE_SMALL));
      icnNode = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_NODE))) : new ImageIcon(Utils.getResource(ICON_NODE));
      icnCpu = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_CPU))) : new ImageIcon(Utils.getResource(ICON_CPU));
      icnNet = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_NET))) : new ImageIcon(Utils.getResource(ICON_NET));
      icnMisc = jar ? new ImageIcon(ImageIO.read(Utils.getResourceFromJar(ICON_SETTINGS))) : new ImageIcon(Utils.getResource(ICON_SETTINGS));
      setIconImage(icnNode.getImage());
    }
    catch (Throwable t) { Log.e(t); }

    ////////////////////////////////////////////////////////////////////////////
    // menus

    final JMenuBar mnuBar = new JMenuBar();

    ///////////////////////////////
    final JMenu mnuFile = new JMenu("File");
    mnuBar.add(mnuFile);

    final JMenuItem mnuSaveSettings = new JMenuItem("Save Settings");
    mnuSaveSettings.addActionListener(l -> saveSettings());
    mnuFile.add(mnuSaveSettings);

    final JMenuItem mnuLoadSettings = new JMenuItem("Load Settings");
    mnuLoadSettings.addActionListener(l -> loadSettings());
    mnuFile.add(mnuLoadSettings);

    ///////////////////////////////
    mnuFile.addSeparator();

    final JMenuItem mnuSendWork = new JMenuItem("Send Completed Work Now");
    mnuSendWork.addActionListener(l -> sendWork());
    mnuFile.add(mnuSendWork);

    final JMenuItem mnuRecvWork = new JMenuItem("Request New Search Root");
    mnuRecvWork.addActionListener(l -> recvWork());
    mnuFile.add(mnuRecvWork);

    ///////////////////////////////
    mnuFile.addSeparator();

    final JMenuItem mnuQuit = new JMenuItem("Quit Discarding Changes");
    mnuQuit.addActionListener(l -> exit(false));
    mnuFile.add(mnuQuit);

    final JMenuItem mnuSaveAndQuit = new JMenuItem("Save & Quit");
    mnuSaveAndQuit.addActionListener(l -> exit());
    mnuFile.add(mnuSaveAndQuit);
    ///////////////////////////////

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

      ///////////////////////////////
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

      ///////////////////////////////
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
    catch (Throwable t) { Log.o("for more info, visit:\n" + ABOUT_URL);  Log.e(t); }

    setJMenuBar(mnuBar);

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

    Log.o("\"Thank you\" raised to the 101st power for helping my semiprime research!");
    Log.o("If you're computer cracks a target number, you will be credited in the publication (assuming you provided an email I can reach you at).");
    Log.o("If you're interested in learning exactly what this software does and why, checkout the \"About\" menu.\n");

    final JScrollPane pneHistory = new JScrollPane(txtHistory);
    txtHistory.setHighlighter(new DefaultHighlighter());
    pneHistory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    pneHistory.setVisible(true);

    // username
    final JLabel lblUsername = getLabel("Optional username:");
    txtUsername = getTextField(System.getProperty("user.name"));

    // email
    final JLabel lblEmail = getLabel("Optional email (in case you crack a number\u2014will never share):");
    txtEmail = getTextField(DEFAULT_EMAIL);

    // host address label and text box
    final JLabel lblAddress = getLabel("Server address:");
    txtAddress = getTextField(DEFAULT_HOST);
    txtAddress.setColumns(DEFAULT_HOST.length());

    // port box and restrict to numbers
    final JLabel lblPort = getLabel("Server port:");
    txtPort = getNumberTextField(""+DEFAULT_PORT);
    txtPort.setColumns(5);

    final JLabel lblConnectNow = getLabel("Click update if you change your username or email after connecting:");

    btnConnect = getButton("Connect Now");
    btnConnect.addActionListener(e -> { if (isConnecting.compareAndSet(false, true)) connect(); });

    btnUpdate = getButton("Update");
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
    pnlNet.add(pneHistory);
    pnlNet.add(pnlConnect);

    ////////////////////////////////////////////////////////////////////////////
    // search tab

    chkFavorPerformance = getCheckBox(FAVOR_PERFORMANCE_NAME, DEFAULT_FAVOR_PERFORMANCE);
    chkFavorPerformance.setToolTipText("<html>CPU performance will be favored, but (possibly lots) more memory will be consumed.<br>Watch caching if you have a solid state drive.</html>");
    chkFavorPerformance.addActionListener((e) -> { Solver.favorPerformance(chkFavorPerformance.isSelected()); });

    chkCompressMemory = getCheckBox(COMPRESS_MEMORY_NAME, DEFAULT_COMPRESS_MEMORY);
    chkCompressMemory.setToolTipText("Memory will be spared, but possibly at great cost to CPU time.");
    chkCompressMemory.addActionListener((e) -> { Solver.compressMemory(chkCompressMemory.isSelected()); });

    chkRestrictDisk = getCheckBox(RESTRICT_DISK_NAME, DEFAULT_RESTRICT_DISK);
    chkRestrictDisk.setToolTipText("Disk I/O enabled or disabled for caching search if/when memory runs low.");
    chkRestrictDisk.addActionListener((e) -> Solver.restrictDisk(chkRestrictDisk.isSelected()));

    /////////////////////////////////////

    chkRestrictNetwork = getCheckBox(RESTRICT_NETWORK_NAME, DEFAULT_RESTRICT_NETWORK);
    chkRestrictNetwork.setToolTipText("Memory will be spared, but possibly at great cost to CPU time.");
    chkRestrictNetwork.addActionListener((e) -> { Solver.restrictNetwork(chkRestrictNetwork.isSelected()); });

    chkPeriodicStats = getCheckBox(PERIODIC_STATS_NAME, DEFAULT_PERIODIC_STATS);
    chkPeriodicStats.setToolTipText("Print stats reflecting search status every so often.");
    chkPeriodicStats.addActionListener((e) -> Solver.periodicStats(chkPeriodicStats.isSelected()));

    chkDetailedStats = getCheckBox(DETAILED_STATS_NAME, DEFAULT_DETAILED_STATS);
    chkDetailedStats.setToolTipText("Calculated detailed stats at great performance and memory cost (use to debug).");
    chkDetailedStats.addActionListener((e) -> Solver.detailedStats(chkDetailedStats.isSelected()));

    /////////////////////////////////////

    chkPrintAllNodes = getCheckBox(PRINT_ALL_NODES_NAME, DEFAULT_PRINT_ALL_NODES);
    chkPrintAllNodes.setToolTipText("<html>All nodes generated and expanded will be printed in the order of occurrence.<br>This will bring search speed to a halt and eat tons of memory for large search spaces!</html>");
    chkPrintAllNodes.addActionListener((e) -> Solver.printAllNodes(chkPrintAllNodes.isSelected()));

    chkWriteCsv = getCheckBox(WRITE_CSV_NAME, DEFAULT_WRITE_CSV);
    chkWriteCsv.setToolTipText("<html>All nodes generated will be written to disk in CSV format in order of occurrence.<br>This may bring search speed to a halt and/or fill your disk!</html>");
    chkWriteCsv.addActionListener((e) -> Solver.writeCsv(chkWriteCsv.isSelected()));

    chkBackground = getCheckBox("work in background", prefs.getBoolean(BACKGROUND_NAME, DEFAULT_BACKGROUND));
    chkBackground.setToolTipText("Only run when system is idle.");
    chkBackground.addActionListener(l -> { Solver.background(chkBackground.isSelected()); Log.o("background: " + (Solver.background() ? "yes" : "no")); });

    /////////////////////////////////////

    final int tblWidth = Math.min(3, Heuristic.values().length);

    final Object[][] table = new Object[Math.max(1,Heuristic.values().length/tblWidth)][tblWidth];
    int row=0, col=0;
    for (Heuristic h : Heuristic.values())
    {
      table[row][col] = h.toString();
      ++col; if (0 == col%tblWidth) { ++row; col=0; }
    }

    tblHeuristics = new JTable(new DefaultTableModel(table,new String[tblWidth]) { @Override public boolean isCellEditable(int r, int c) { return false; } });
    tblHeuristics.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
    tblHeuristics.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    tblHeuristics.setCellSelectionEnabled(true);

    lblSemiprime = getLabel("Local Semiprime Target");
    lblSemiprime.setIcon(icnNodeSmall);

    txtSemiprime = new JTextArea(HISTORY_ROWS, HISTORY_COLS);
    txtSemiprime.setHighlighter(new DefaultHighlighter());
    txtSemiprime.addKeyListener(new KeyListener()
    {
      @Override public void keyTyped(KeyEvent e) {}
      @Override public void keyPressed(KeyEvent e) {}
      @Override public void keyReleased(KeyEvent e)
      {
        // grab the inputted target value and strip formatting
        final String s = clean(txtSemiprime.getText()).toLowerCase();

        // the assumptions made here regarding base are just to help speed up selecting settings,
        // they are by *no means* meant to account for all "base-cases", which wouldn't be possible
        // to do correctly in all cases w/o user-input anyway
        boolean allBinaryDigits = true, containsHex = false, whitespaceChange = true;
        for (char c : s.toCharArray())
        {
          if (Character.isWhitespace(c)) continue; else whitespaceChange = false;
          if ('a' == c || 'b' == c || 'c' == c || 'd' == c || 'e' == c || 'f' == c) { containsHex = true; break; }
          else if (c != '0' && c != '1') { allBinaryDigits = false; }
        }

        // if the change wasn't trivial...
        if (!whitespaceChange)
        {
          // try to guess the base
          final String prevBase = clean(txtSemiprimeBase.getText());
          if (containsHex) txtSemiprimeBase.setText("16");
          else if (allBinaryDigits) txtSemiprimeBase.setText("2");
          else txtSemiprimeBase.setText(""+DEFAULT_SEMIPRIME_BASE);

          // further ensure value change and clear old prime lengths
          if (!prevBase.equals(clean(txtSemiprimeBase.getText()))) { txtP1Len.setText("0"); txtP2Len.setText("0"); }
          updateSemiprimeHeader();
        }
      }
    });

    final JScrollPane pneSemirpime = new JScrollPane(txtSemiprime);
    pneSemirpime.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    pneSemirpime.setVisible(true);

    /////////////////////////////////////

    final String semiprimeBaseHelp = "The base that you are providing your semiprime in.";
    final String internalBaseHelp = "The base that will be used internally by the solver.";
    final String primeLengthHelp = "Measured in digits of internal base.\nUse 0 if unknown.";

    final JLabel lblSemiprimeBase = getLabel("Semiprime Base");
    lblSemiprimeBase.setToolTipText(semiprimeBaseHelp);
    final JLabel lblInternalBase = getLabel("Internal Base");
    lblInternalBase.setToolTipText(internalBaseHelp);

    txtSemiprimeBase = getNumberTextField("10");
    txtSemiprimeBase.setToolTipText(semiprimeBaseHelp);

    txtInternalBase = getNumberTextField("2");
    txtInternalBase.setEnabled(false);
    txtInternalBase.setToolTipText(internalBaseHelp);

    final JLabel lblP1Len = getLabel("Prime 1 Length");
    lblP1Len.setToolTipText(primeLengthHelp);
    final JLabel lblP2Len = getLabel("Prime 2 Length");
    lblP2Len.setToolTipText(primeLengthHelp);

    txtP1Len = getNumberTextField("0");
    txtP1Len.setToolTipText(primeLengthHelp);
    txtP1Len.addKeyListener(new KeyListener()
    {
      @Override public void keyTyped(KeyEvent e) {}
      @Override public void keyPressed(KeyEvent e) {}
      @Override public void keyReleased(KeyEvent e)
      {
        try
        {
          final int len1 = Integer.parseInt(txtP1Len.getText().trim());
          if (len1 >= 0)
          {
            Solver.prime1Len(len1); Solver.prime2Len(0 != len1 ? getSemiprimeLen() - len1 : 0);
            txtP2Len.setText(""+Solver.prime2Len());
          }
        }
        catch (Throwable ignored) {}
      }
    });

    txtP2Len = getNumberTextField("0");
    txtP2Len.setToolTipText(primeLengthHelp);
    txtP2Len.addActionListener((e) -> { try { Solver.prime2Len(Integer.parseInt(txtP2Len.getText().trim())); } catch (Throwable ignored) {} });

    final JButton btnReset = getButton("Reset to Defaults");
    btnReset.setToolTipText("This will reset the search settings to defaults (w/o clearing the current semiprime value).");
    btnReset.addActionListener((e) -> resetSearchSettings());

    final JButton btnRsa100 = getButton("RSA-100");
    btnRsa100.setToolTipText("Loads a pre-selected benchmark to run against.");
    btnRsa100.addActionListener((e) -> loadBenchmark(RSA_100));

    final JButton btnRsa220 = getButton("RSA-220");
    btnRsa220.setToolTipText("Loads a pre-selected benchmark to run against.");
    btnRsa220.addActionListener((e) -> loadBenchmark(RSA_220));

    final JButton btnRsa300 = getButton("RSA-300");
    btnRsa300.setToolTipText("Loads a pre-selected benchmark to run against.");
    btnRsa300.addActionListener((e) -> loadBenchmark(RSA_300));

    final JButton btnRsa2048 = getButton("RSA-2048");
    btnRsa2048.setToolTipText("Loads a pre-selected benchmark to run against.");
    btnRsa2048.addActionListener((e) -> loadBenchmark(RSA_2048));

    final JButton btnRsaLen = getButton("Calc. N/2 (assumes fixed length primes)");
    btnRsaLen.setToolTipText("If you know the lengths of your primes in advance, you can greatly aid the search.");
    btnRsaLen.addActionListener((e) -> { try { final int len = getSemiprimeLen(); txtP1Len.setText(""+((len/2)+(0==len%2?0:1))); txtP2Len.setText(""+((len/2)+(0==len%2?0:1))); } catch (Throwable ignored) {} });

    /////////////////////////////////////

    btnSearch = getButton("Start Local Search");
    btnSearch.addActionListener(e ->
    {
      try
      {
        // prevent multiple clicks
        btnSearch.setEnabled(false);

        // interrupt any previous solver and wait for termination
        if (!isSearching.compareAndSet(false, true))
        {
          final Thread prev = solverThread(null);
          if (null != prev) { Log.o("interrupting search..."); Solver.interrupt(); prev.join(); return; }
        }

        // move to main screen to view search progress or init errors
        btnSearch.setText("Preparing search...");

        // reset the solver for a new search
        Solver.reset();
        Solver.periodicStats(chkPeriodicStats.isSelected());
        Solver.detailedStats(chkDetailedStats.isSelected());
        Solver.favorPerformance(chkFavorPerformance.isSelected());
        Solver.compressMemory(chkCompressMemory.isSelected());
        Solver.restrictDisk(chkRestrictDisk.isSelected());
        Solver.restrictNetwork(chkRestrictNetwork.isSelected());
        Solver.background(chkBackground.isSelected());
        Solver.printAllNodes(chkPrintAllNodes.isSelected());
        Solver.writeCsv(chkWriteCsv.isSelected());
        Solver.processors(sldProcessors.getValue());
        Solver.processorCap(sldProcessorCap.getValue());
        Solver.memoryCap(sldMemoryCap.getValue());

        // set heuristics
        Solver.heuristics().clear();
        for (int i : tblHeuristics.getSelectedRows())
            for (int j : tblHeuristics.getSelectedColumns())
                Solver.addHeuristic(Heuristic.byName((String) table[i][j]));

        // set callback for search completion
        Solver.callback(n ->
        {
          // null == solverThread() -> search was cancelled before completion
          if (null != n) { pneMain.setSelectedIndex(TAB_CONNECT); Log.o("\nsearch complete:\n\n\tsp:\t" + n.product(10) + " (" + n.product() + ")\n\tp1:\t" + n.factor(0) + " (" + n.factor(0).toString(Solver.internalBase()) + ")\n\tp2:\t" + n.factor(1) + " (" + n.factor(1).toString(Solver.internalBase()) + ")"); }
          else if (null != solverThread()) { pneMain.setSelectedIndex(TAB_CONNECT); Log.e("search complete:\n\n\tno factors could be found, are you sure the input is composite" + (Solver.primeLengthsFixed() ? " and the factors are the specified lengths" : "") + "?"); }
          isSearching.set(false);
          btnSearch.setText("Start Local Search");
        });

        // try to parse any fixed prime lengths
        try { Solver.prime1Len(Integer.parseInt(clean(txtP1Len.getText()))); } catch (Throwable t) { Log.e("prime 1 len invalid"); return; }
        try { Solver.prime2Len(Integer.parseInt(clean(txtP2Len.getText()))); } catch (Throwable t) { Log.e("prime 2 len invalid"); return; }

        // grab the semiprime options
        int spBase = DEFAULT_SEMIPRIME_BASE;
        try { spBase = Integer.parseInt(clean(txtSemiprimeBase.getText())); } catch (Throwable t) { Log.e("semiprime base invalid"); txtSemiprimeBase.setText(""+DEFAULT_SEMIPRIME_BASE); return; }
        if (spBase < 2) { Log.e("semiprime base < 2"); txtSemiprimeBase.setText(""+DEFAULT_SEMIPRIME_BASE); return; }

        int internalBase = DEFAULT_INTERNAL_BASE;
        try { internalBase = Integer.parseInt(clean(txtInternalBase.getText())); }
        catch (Throwable t) { Log.e("provided internal base was invalid, defaulting to " + DEFAULT_INTERNAL_BASE); txtInternalBase.setText(""+DEFAULT_INTERNAL_BASE); }
        if (internalBase < 2) { Log.e("internal base cannot be < 2, defaulting to " + DEFAULT_INTERNAL_BASE); txtInternalBase.setText(""+DEFAULT_INTERNAL_BASE); return; }

        // create a new solver based upon user request and launch it
        solverThread(new Thread(Solver.newInstance(clean(txtSemiprime.getText()), spBase, internalBase)));

        // ensure gui values reflect underlying state
        updateSettings();

        // in case search crashes app due to internal error or user picking bad flags
        saveSettings();

        // prevent multiple searches
        btnSearch.setText("Cancel Search");
      }
      catch (Throwable t) { Log.e(t); }
      finally
      {
        final Thread thread = solverThread();
        if (null != thread) thread.start(); else isSearching.set(false);
        btnSearch.setEnabled(true);
      }
    });

    /////////////////////////////////////

    final JPanel pnlSearchOptions = new JPanel(new GridLayout(3,3));
    pnlSearchOptions.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            "Search Options",
            TitledBorder.CENTER,
            TitledBorder.TOP
    ));
    pnlSearchOptions.add(chkFavorPerformance);
    pnlSearchOptions.add(chkCompressMemory);
    pnlSearchOptions.add(chkRestrictDisk);
    pnlSearchOptions.add(chkRestrictNetwork);
    pnlSearchOptions.add(chkPeriodicStats);
    pnlSearchOptions.add(chkDetailedStats);
    pnlSearchOptions.add(chkPrintAllNodes);
    pnlSearchOptions.add(chkWriteCsv);
    pnlSearchOptions.add(chkBackground);

    final JPanel pnlHeuristics = new JPanel(new GridLayout(1,1));
    pnlHeuristics.add(tblHeuristics);
    pnlHeuristics.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(),
            "Select Heuristics",
            TitledBorder.CENTER,
            TitledBorder.TOP
    ));

    final JPanel pnlHeader = new JPanel(new GridLayout(2,1,H_GAP,V_GAP));
    pnlHeader.add(pnlSearchOptions);
    pnlHeader.add(pnlHeuristics);

    final JPanel pnlSemiprime = new JPanel(new GridLayout(1,1));
    pnlSemiprime.add(pneSemirpime);
    pnlSemiprime.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(),
            "Local Semiprime Target",
            TitledBorder.CENTER,
            TitledBorder.TOP
    ));

    final JPanel pnlSemiprimeOptions = new JPanel(new GridLayout(4,2));
    pnlSemiprimeOptions.add(lblSemiprimeBase);
    pnlSemiprimeOptions.add(txtSemiprimeBase);
    pnlSemiprimeOptions.add(lblInternalBase);
    pnlSemiprimeOptions.add(txtInternalBase);
    pnlSemiprimeOptions.add(lblP1Len);
    pnlSemiprimeOptions.add(txtP1Len);
    pnlSemiprimeOptions.add(lblP2Len);
    pnlSemiprimeOptions.add(txtP2Len);

    final JPanel pnlBenchmark = new JPanel(new GridLayout(2,2));
    pnlBenchmark.add(btnRsa100);
    pnlBenchmark.add(btnRsa220);
    pnlBenchmark.add(btnRsa300);
    pnlBenchmark.add(btnRsa2048);

    final JPanel pnlButtons0 = new JPanel(new GridLayout(1,2));
    pnlButtons0.add(btnReset);
    pnlButtons0.add(pnlBenchmark);

    final JPanel pnlButtons1 = new JPanel(new GridLayout(1,2));
    pnlButtons1.add(pnlButtons0);
    pnlButtons1.add(btnRsaLen);

    final JPanel pnlButtons2 = new JPanel(new GridLayout(1,1));
    pnlButtons2.add(btnSearch);

    final JPanel pnlButtons = new JPanel(new GridLayout(2,1));
    pnlButtons.add(pnlButtons1);
    pnlButtons.add(pnlButtons2);

    final JPanel pnlFooter = new JPanel(new GridLayout(2,1));
    pnlFooter.add(pnlSemiprimeOptions);
    pnlFooter.add(pnlButtons);

    final JPanel pnlSearch = new JPanel(new GridLayout(3,1, H_GAP, V_GAP));
    pnlSearch.add(pnlHeader);
    pnlSearch.add(pnlSemiprime);
    pnlSearch.add(pnlFooter);

    ////////////////////////////////////////////////////////////////////////////
    // cpu tab

    // setup the memory/ processing limit sliders:
    final JLabel lblProcessors = getLabel("Processors to use");
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
      Log.o("processors: " + val);
    });

    final JLabel lblCap = getLabel("Per-processor usage limit (%)");
    sldProcessorCap = new JSlider(0, 100, prefs.getInt(PROCESSOR_CAP_NAME, DEFAULT_PROCESSOR_CAP));
    sldProcessorCap.setMajorTickSpacing(25);
    sldProcessorCap.setMinorTickSpacing(5);
    sldProcessorCap.setSnapToTicks(true);
    sldProcessorCap.setPaintLabels(true);
    sldProcessorCap.setPaintTicks(true);
    sldProcessorCap.addChangeListener(c ->
    {
      if (sldProcessorCap.getValueIsAdjusting()) return;
      int val = sldProcessorCap.getValue();
      Solver.processorCap(val);
      Log.o("processorCap: " + val + "%");
    });

    final JLabel lblMemory = getLabel("Memory usage limit (%)");
    sldMemoryCap = new JSlider(0, 100, prefs.getInt(MEMORY_CAP_NAME, DEFAULT_MEMORY_CAP));
    sldMemoryCap.setMajorTickSpacing(25);
    sldMemoryCap.setMinorTickSpacing(5);
    sldMemoryCap.setSnapToTicks(true);
    sldMemoryCap.setPaintLabels(true);
    sldMemoryCap.setPaintTicks(true);
    sldMemoryCap.addChangeListener(c ->
    {
      if (sldMemoryCap.getValueIsAdjusting()) return;
      int val = sldMemoryCap.getValue();
      Solver.memoryCap(val);
      Log.o("memoryCap: " + val + "%");
    });

    final JLabel lblIdle = getLabel("Idle time until work begins (min)");
    sldIdle = new JSlider(0, 30, prefs.getInt(IDLE_MINUTES_NAME, DEFAULT_IDLE_MINUTES));
    sldIdle.setMajorTickSpacing(5);
    sldIdle.setMinorTickSpacing(1);
    sldIdle.setSnapToTicks(true);
    sldIdle.setPaintLabels(true);
    sldIdle.setPaintTicks(true);
    sldIdle.addChangeListener(c ->
    {
      if (sldIdle.getValueIsAdjusting()) return;
      int val = sldIdle.getValue();
      Log.o("idle delay before search: " + val + " minutes");
    });

    final JButton btnResetCpu = getButton("Reset to Defaults");
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
    pnlCpuLeft.add(sldProcessorCap);
    pnlCpuLeft.add(getLabel(""));
    pnlCpuLeft.add(getLabel(""));
    pnlCpuLeft.add(btnResetCpu);

    // setup the right-side:
    final JPanel pnlCpuRight = new JPanel(new GridLayout(7, 1, H_GAP, V_GAP));

    // auto start with system?
    chkAutoStart = getCheckBox("auto-start with system", prefs.getBoolean(AUTOSTART_NAME, DEFAULT_AUTOSTART));
    chkAutoStart.addActionListener(l -> Log.o("autostart: " + (chkAutoStart.isSelected() ? "yes" : "no")));

    final JPanel pnlChkBoxes = new JPanel(new GridLayout(1, 1, H_GAP, V_GAP));
    pnlChkBoxes.add(chkAutoStart);

    pnlCpuRight.add(lblMemory);
    pnlCpuRight.add(sldMemoryCap);
    pnlCpuRight.add(lblIdle);
    pnlCpuRight.add(sldIdle);
    pnlCpuRight.add(getLabel(""));
    pnlCpuRight.add(getLabel(""));
    pnlCpuRight.add(pnlChkBoxes);

    // create the full CPU panel
    final JPanel pnlCpu = new JPanel(new GridLayout(1, 2, H_GAP, V_GAP));
    pnlCpu.add(pnlCpuLeft);
    pnlCpu.add(pnlCpuRight);

    ////////////////////////////////////////////////////////////////////////////
    // miscellaneous tab

    final JPanel pnlMisc = new JPanel(new GridLayout(1,1));
    pnlMisc.add(getLabel("You will find additional settings here when they become available."));

    ////////////////////////////////////////////////////////////////////////////
    // add tabs to frame

    // create the tabbed panes
    pneMain = new JTabbedPane();
    pneMain.setFocusable(false);
    pneMain.setBorder(null);
    pneMain.addTab("", icnNet, pnlNet, "Connect to a compute server");
    pneMain.addTab("", icnNode, pnlSearch, "Search settings");
    pneMain.addTab("", icnCpu, pnlCpu, "Hardware settings");
    pneMain.addTab("", icnMisc, pnlMisc, "Miscellaneous settings");

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

      final MenuItem quit = new MenuItem("Quit Discarding Changes");
      quit.addActionListener(l -> exit(false));

      final MenuItem saveAndQuit = new MenuItem("Save & Quit");
      saveAndQuit.addActionListener(l -> exit());

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
      popup.add(saveAndQuit);

      trayIcon.setPopupMenu(popup);

      try { systemTray.add(trayIcon); } catch (Throwable t) { useTray = false; Log.e("couldn't create tray icon, will exit on window close immediately"); }
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
    Log.o(
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

  private int getSemiprimeLen()
  {
    try
    {
      final int spBase = Integer.parseInt(clean(txtSemiprimeBase.getText()));
      final int internalBase = Integer.parseInt(clean(txtInternalBase.getText()));
      return new BigInteger(clean(txtSemiprime.getText()), spBase).toString(internalBase).length();
    }
    catch (Throwable t) { return 0; }
  }

  private void updateSemiprimeHeader()
  {
    tblHeuristics.getTableHeader().setName("Local Semiprime Target (len: " + clean(txtSemiprime.getText()).length() + ", internal len: " + getSemiprimeLen() + ")");
  }

  private void loadBenchmark(String benchmark)
  {
    txtSemiprime.setText(benchmark);
    txtSemiprimeBase.setText("10");
    txtInternalBase.setText("2");
    final String len = ""+((getSemiprimeLen()/2)+(0==getSemiprimeLen()%2?0:1));
    txtP1Len.setText(len); txtP2Len.setText(len);
    updateSemiprimeHeader();
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

  private void pause()
  {
    Log.o("pausing all work...");

    Log.o("all work paused");
  }

  private void resume()
  {
    Log.o("resuming all work...");

    Log.o("all work resumed");
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
    Log.o("sending user settings to the server...");

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
    client.username(username);
    if (!client.write(Packet.Type.USERNAME_UPDATE, username)) { Log.e("failed to send username, try reconnecting"); return; }

    client.email(email);
    if (!client.write(Packet.Type.EMAIL_UPDATE, email)) { Log.e("failed to send email, try reconnecting"); return; }

    // if all went all, update our state
    txtEmail.setEnabled(true);
    txtUsername.setEnabled(true);
    btnUpdate.setEnabled(true);
    isUpdatePending.set(false);

    Log.o("the server has acknowledged your settings");
  }

  private void saveCpuSettings()
  {
    prefs.putInt(PROCESSORS_NAME, sldProcessors.getValue());
    prefs.putInt(PROCESSOR_CAP_NAME, sldProcessorCap.getValue());
    prefs.putInt(MEMORY_CAP_NAME, sldMemoryCap.getValue());
    prefs.putInt(IDLE_MINUTES_NAME, sldIdle.getValue());

    prefs.putBoolean(BACKGROUND_NAME, chkBackground.isSelected());
    prefs.putBoolean(AUTOSTART_NAME, chkAutoStart.isSelected());

    Log.o("cpu settings saved");
  }

  private void saveSearchSettings()
  {
    prefs.putByteArray(SEMIPRIME_NAME, txtSemiprime.getText().trim().getBytes());

    int spBase = prefs.getInt(SEMIPRIME_BASE_NAME, DEFAULT_SEMIPRIME_BASE);
    try { spBase = Integer.parseInt(txtSemiprimeBase.getText().trim()); } catch (Throwable ignored) {}
    prefs.putInt(SEMIPRIME_BASE_NAME, spBase);

    int internalBase = prefs.getInt(INTERNAL_BASE_NAME, DEFAULT_INTERNAL_BASE);
    try { internalBase = Integer.parseInt(txtInternalBase.getText().trim()); } catch (Throwable ignored) {}
    prefs.putInt(INTERNAL_BASE_NAME, internalBase);

    int p1Len = prefs.getInt(P1_LEN_NAME, DEFAULT_P1_LEN);
    try { p1Len = Integer.parseInt(txtP1Len.getText().trim()); } catch (Throwable ignored) {}
    prefs.putInt(P1_LEN_NAME, p1Len);

    int p2Len = prefs.getInt(P2_LEN_NAME, DEFAULT_P2_LEN);
    try { p2Len = Integer.parseInt(txtP2Len.getText().trim()); } catch (Throwable ignored) {}
    prefs.putInt(P2_LEN_NAME, p2Len);

    prefs.putBoolean(FAVOR_PERFORMANCE_NAME, chkFavorPerformance.isSelected());
    prefs.putBoolean(COMPRESS_MEMORY_NAME, chkCompressMemory.isSelected());
    prefs.putBoolean(RESTRICT_DISK_NAME, chkRestrictDisk.isSelected());
    prefs.putBoolean(PRINT_ALL_NODES_NAME, chkPrintAllNodes.isSelected());
    prefs.putBoolean(WRITE_CSV_NAME, chkWriteCsv.isSelected());
  }


  private void saveSettings()
  {
    Log.o("saving settings...");

    saveSearchSettings();
    saveCpuSettings();
    try { prefs.flush(); } catch (Throwable ignored) {} // this always generates an error on windows, but works

    Log.o("all settings saved");
  }

  private void loadCpuSettings()
  {
    final int processors = prefs.getInt(PROCESSORS_NAME, DEFAULT_PROCESSORS);
    sldProcessors.setValue(processors);
    Solver.processors(processors);

    final int processorCap = prefs.getInt(PROCESSOR_CAP_NAME, DEFAULT_PROCESSOR_CAP);
    sldProcessorCap.setValue(processorCap);
    Solver.processorCap(processorCap);

    final int memory = prefs.getInt(MEMORY_CAP_NAME, DEFAULT_MEMORY_CAP);
    sldMemoryCap.setValue(memory);
    Solver.memoryCap(memory);

    sldIdle.setValue(prefs.getInt(IDLE_MINUTES_NAME, DEFAULT_IDLE_MINUTES));

    chkBackground.setSelected(prefs.getBoolean(BACKGROUND_NAME, DEFAULT_BACKGROUND));
    chkAutoStart.setSelected(prefs.getBoolean(AUTOSTART_NAME, DEFAULT_AUTOSTART));

    Log.o("cpu settings loaded");
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
    updateSemiprimeHeader();

    txtSemiprimeBase.setText(""+prefs.getInt(SEMIPRIME_BASE_NAME, DEFAULT_SEMIPRIME_BASE));
    txtInternalBase.setText(""+prefs.getInt(INTERNAL_BASE_NAME, DEFAULT_INTERNAL_BASE));
    txtP1Len.setText(""+prefs.getInt(P1_LEN_NAME, DEFAULT_P1_LEN));
    txtP2Len.setText(""+prefs.getInt(P2_LEN_NAME, DEFAULT_P2_LEN));

    chkFavorPerformance.setSelected(prefs.getBoolean(FAVOR_PERFORMANCE_NAME, DEFAULT_FAVOR_PERFORMANCE));
    chkCompressMemory.setSelected(prefs.getBoolean(COMPRESS_MEMORY_NAME, DEFAULT_COMPRESS_MEMORY));
    chkRestrictDisk.setSelected(prefs.getBoolean(RESTRICT_DISK_NAME, DEFAULT_RESTRICT_DISK));
    chkPrintAllNodes.setSelected(prefs.getBoolean(PRINT_ALL_NODES_NAME, DEFAULT_PRINT_ALL_NODES));
    chkWriteCsv.setSelected(prefs.getBoolean(WRITE_CSV_NAME, DEFAULT_WRITE_CSV));

    Log.o("search settings loaded");
  }

  private void loadSettings()
  {
    try
    {
      Log.o("loading settings...");
      prefs = Preferences.userNodeForPackage(getClass());
      loadCpuSettings();
      loadSearchSettings();
      Log.o("all settings loaded successfully");
    }
    catch (Throwable t) { Log.e("failed to load settings. make sure app has read permissions"); return; }
  }

  private void updateSearchSettings()
  {
    chkFavorPerformance.setSelected(Solver.favorPerformance());
    chkCompressMemory.setSelected(Solver.compressMemory());
    chkRestrictDisk.setSelected(Solver.restrictDisk());
    chkPrintAllNodes.setSelected(Solver.printAllNodes());
    chkWriteCsv.setSelected(Solver.writeCsv());

    txtInternalBase.setText(""+Solver.internalBase());
    txtP1Len.setText(""+Solver.prime1Len());
    txtP2Len.setText(""+Solver.prime2Len());
  }

  private void updateSettings()
  {
    updateSearchSettings();
  }

  private void resetCpuSettings()
  {
    sldProcessors.setValue(DEFAULT_PROCESSORS);
    sldProcessorCap.setValue(DEFAULT_PROCESSOR_CAP);
    sldMemoryCap.setValue(DEFAULT_MEMORY_CAP);
    sldIdle.setValue(DEFAULT_IDLE_MINUTES);

    chkBackground.setSelected(DEFAULT_BACKGROUND);
    chkAutoStart.setSelected(DEFAULT_AUTOSTART);

    Log.o("cpu settings reset");
  }

  private void resetSearchSettings()
  {
    txtSemiprime.setText("");
    updateSemiprimeHeader();

    txtSemiprimeBase.setText(""+DEFAULT_SEMIPRIME_BASE);
    txtInternalBase.setText(""+DEFAULT_INTERNAL_BASE);
    txtP1Len.setText(""+DEFAULT_P1_LEN);
    txtP2Len.setText(""+DEFAULT_P2_LEN);

    chkFavorPerformance.setSelected(DEFAULT_FAVOR_PERFORMANCE);
    chkCompressMemory.setSelected(DEFAULT_COMPRESS_MEMORY);
    chkRestrictDisk.setSelected(DEFAULT_RESTRICT_DISK);
    chkPrintAllNodes.setSelected(DEFAULT_PRINT_ALL_NODES);
    chkWriteCsv.setSelected(DEFAULT_WRITE_CSV);

    Log.o("search settings reset");
  }

  private void resetSettings()
  {
    Log.o("resetting all settings to defaults...");

    resetSearchSettings();
    resetCpuSettings();

    Log.o("settings reset");
  }

  private void sendWork()
  {
    Log.o("sending all completed work to server...");

    Log.o("server successfully received all completed work");
  }

  private void recvWork()
  {
    Log.o("requesting a new primary node...");

    Log.o("new workload received; restarting search...");
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

  private static String clean(String s) { return null != s ? s.replace(" ", "").replace("\t","").replace("\n","").replace("\r","").trim() : "";}

  private static final DocumentFilter numberFilter = new DocumentFilter()
  {
    final Pattern regEx = Pattern.compile("\\d+");
    @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
    {
      if (!regEx.matcher(text).matches()) return;
      super.replace(fb, offset, length, text, attrs);
    }
  };

  private static JCheckBox getCheckBox(String s, boolean isChecked)
  {
    final JCheckBox checkBox = new JCheckBox(s, isChecked);
    checkBox.setHorizontalAlignment(SwingConstants.CENTER);
    checkBox.setFocusPainted(false);
    return checkBox;
  }

  private static JButton getButton(String s)
  {
    final JButton button = new JButton(s);
    button.setHorizontalAlignment(SwingConstants.CENTER);
    button.setFocusPainted(false);
    return button;
  }

  private static JLabel getLabel(String s)
  {
    final JLabel label = new JLabel(s);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }

  private static JTextField getTextField(String s)
  {
    final JTextField txtField = new JTextField(s);
    txtField.setHorizontalAlignment(SwingConstants.CENTER);
    return txtField;
  }

  private static JTextField getNumberTextField(String s) { return getNumberTextField(s, 2); }
  private static JTextField getNumberTextField(String s, int columns)
  {
    final JTextField txtNumberField = getTextField(s);
    ((AbstractDocument)txtNumberField.getDocument()).setDocumentFilter(numberFilter);
    txtNumberField.setColumns(columns);
    return txtNumberField;
  }
}
