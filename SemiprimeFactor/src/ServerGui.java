/**
 * Created by Stephen on 11/2/2015.
 */
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * Created by Stephen on 11/1/2015.
 */
public class ServerGui extends JFrame implements DocumentListener
{
  public static final String DEFAULT_TITLE = "Semiprime Factorization Server - " + ClientGui.VERSION;
  public static final int DEFAULT_WIDTH = 800, DEFAULT_HEIGHT = 600;
  public static final int HISTORY_ROWS = 10, HISTORY_COLS = 20;

  public JPanel mainPanel;
  public JTextArea  txtHistory;
  public JScrollPane scrollPaneHistory;

  private final Server server;

  public ServerGui()
  {
    super();
    setVisible(false);

    if (!create()) throw new NullPointerException();
    server = new Server();

    setVisible(true);
    toFront();
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
      txtHistory.append(s + "\n");
      txtHistory.setCaretPosition(txtHistory.getText().length()-1);
    });

    scrollPaneHistory = new JScrollPane(txtHistory);
    scrollPaneHistory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPaneHistory.setVisible(true);
    
    DefaultHighlighter highlighter = new DefaultHighlighter();
    txtHistory.setHighlighter(highlighter);

    // organize them and add them to the panel
    mainPanel = new JPanel(new GridLayout(1,1));
    mainPanel.add(scrollPaneHistory);

    // add the panel to the frame and show everything
    getContentPane().add(mainPanel);
    resetFrame();

    addWindowListener(new java.awt.event.WindowAdapter()
    {
      @Override
      public void windowClosing(final WindowEvent winEvt)
      {
        server.destroy();
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
