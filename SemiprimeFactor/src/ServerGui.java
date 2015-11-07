/**
 * Created by Stephen on 11/2/2015.
 */
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Stephen on 11/1/2015.
 */
public class ServerGui extends JFrame implements DocumentListener
{
  public static final String DEFAULT_TITLE = "Semiprime Factorization Server - v0.1a";
  public static final int DEFAULT_WIDTH = 800, DEFAULT_HEIGHT = 600;
  public static final int DEFAULT_TXT_ROWS = 5, DEFAULT_TXT_COLS = 20;
  private static final String BTN_CONNECT_STRING = "Connect to Stephen's server.";

  public JPanel mainPanel;
  public JTextArea  txtHistory;
  public JButton btnWork;
  public JScrollPane scrollPaneHistory;

  private final Server server;
  private final AtomicReference<Client> client = new AtomicReference<>();
  private final AtomicBoolean isConnecting = new AtomicBoolean(false);

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
    btnWork = new JButton();

    txtHistory.setRows(DEFAULT_TXT_ROWS);
    txtHistory.setColumns(DEFAULT_TXT_COLS);
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

    JPanel btnPanel = new JPanel(new GridLayout(2,1));
    btnPanel.add(btnWork);

    DefaultHighlighter highlighter = new DefaultHighlighter();
    txtHistory.setHighlighter(highlighter);

    btnWork.setVisible(false);
    btnWork.setEnabled(false);
    btnWork.addActionListener((event) ->
    {

    });

    // organize them and add them to the panel
    mainPanel = new JPanel(new GridLayout(2,1));
    mainPanel.add(scrollPaneHistory);
    mainPanel.add(btnPanel);

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
