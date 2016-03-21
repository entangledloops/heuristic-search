package com.entangledloops.heuristicsearch.semiprime.server;

import com.entangledloops.heuristicsearch.semiprime.Log;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.net.ServerSocket;

/**
 * @author Stephen Dunn
 * @since November 1, 2015
 */
public class ServerGui extends JFrame implements DocumentListener
{
  //////////////////////////////////////////////////////////////////////////////
  //
  // Constants
  //
  //////////////////////////////////////////////////////////////////////////////

  private static final String VERSION        = "0.3.1a";
  private static final String DEFAULT_TITLE  = "Semiprime Factorization Server - " + VERSION;
  private static final int    DEFAULT_WIDTH  = 800;
  private static final int    DEFAULT_HEIGHT = 600;
  private static final int    HISTORY_ROWS   = 10;
  private static final int    HISTORY_COLS   = 20;

  //////////////////////////////////////////////////////////////////////////////
  //
  // Gui vars
  //
  //////////////////////////////////////////////////////////////////////////////

  private JTextArea txtHistory;

  //////////////////////////////////////////////////////////////////////////////
  //
  // State vars
  //
  //////////////////////////////////////////////////////////////////////////////

  private final Server server;

  //////////////////////////////////////////////////////////////////////////////
  //
  // ServerGui
  //
  //////////////////////////////////////////////////////////////////////////////

  public ServerGui() { this(new Server()); }
  public ServerGui(Server server)
  {
    super();
    this.server = server;

    setVisible(false);
    if (!create()) throw new NullPointerException("failed to create server gui");
    setVisible(true);

    toFront();
  }

  public ServerSocket socket() { return server.socket(); }

  public boolean ready() { return server.ready(); }

  public void resetFrame()
  {
    setTitle(DEFAULT_TITLE);
    setState(Frame.NORMAL);
    setUndecorated(false);
    setResizable(true);
    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    setLocationRelativeTo(getRootPane());
  }

  // re-initializes the window's components
  public boolean create()
  {
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
      //noinspection EmptyCatchBlock
      try
      {
        if (SwingUtilities.isEventDispatchThread()) SwingUtilities.invokeLater(append);
        else append.run();
      }
      catch (Throwable t) {} // don't care what went wrong with gui update, it's been logged anyway
    });

    final JScrollPane scrollPaneHistory = new JScrollPane(txtHistory);
    scrollPaneHistory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPaneHistory.setVisible(true);

    DefaultHighlighter highlighter = new DefaultHighlighter();
    txtHistory.setHighlighter(highlighter);

    // organize them and add them to the panel
    final JPanel mainPanel = new JPanel(new GridLayout(1, 1));
    mainPanel.add(scrollPaneHistory);

    // add the panel to the frame and show everything
    getContentPane().add(mainPanel);
    resetFrame();

    addWindowListener(new java.awt.event.WindowAdapter()
    {
      @Override
      public void windowClosing(final WindowEvent winEvt)
      {
        server.close();
        System.exit(0);
      }
    });

    return true;
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
