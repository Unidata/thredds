/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;
import ucar.ui.prefs.Debug;

/**
 * A simple HTML Browser based on JEditPane.
 *
 * @author John Caron
 */

public class HtmlBrowser extends JPanel {
  private static EditorKit kit = JEditorPane.createEditorKitForContentType("text/html");
  private PreferencesExt prefs;
  private RootPaneContainer parent = null;

  private boolean eventsOK = true;

    // ui
  private JEditorPane htmlViewer;
  private ComboBox cbox;
  private JLabel statusText;
  private JPanel topButtons;
  private AbstractAction backAction, forwardAction;

  private ArrayList nav = new ArrayList(); // list of Page objetcs
  private int currentPage = -1;

  private boolean debug = false, debugDoc = false, showEvent = false;

  /* Implementation notes:
     1. The JEditorPane contains a Document, which is replaced only when the content type changes.
        This causes some history effects. Currently seems to be related only to the Document properties
        dumped out by Page.showDoc().
     2. The "charset=iso-8859-1" makes the document unshowable unless "IgnoreCharsetDirective" = true.
     3. The setText does not reset "stream", so the view may not refresh if it thinks the stream hasnt changed.
   */

  /**
   * Constructor.
   */
  public HtmlBrowser() {
    // this.prefs = prefs;

    // combo box holds Page object
    cbox = new ComboBox(null);
    cbox.setAction( new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        //System.out.println("From catListBox Action " + e+" "+eventsOK);
        if (!eventsOK) return;
        System.out.println("cbox= " + cbox.getSelectedItem());
        if (e.getActionCommand().equals("comboBoxChanged")) {
          Object selected = cbox.getSelectedItem();
          if (selected instanceof String)
            setUrlString( (String) selected); // user typed it in
          if (selected instanceof Page)
            setCurrentPage((Page) selected);
        }
      }
     });

    // top buttons
    topButtons = new JPanel( new FlowLayout(FlowLayout.LEFT, 5, 0));
    backAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { decrCurrentPage(); }
    };
    BAMutil.setActionProperties( backAction, "Left", "Back", false, 'B', 0);
    BAMutil.addActionToContainer( topButtons, backAction);

    forwardAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { incrCurrentPage(); }
    };
    BAMutil.setActionProperties( forwardAction, "Right", "Forward", false, 'F', 0);
    BAMutil.addActionToContainer( topButtons, forwardAction);
    enableActions();

    // top panel
    JPanel topPanel = new JPanel( new BorderLayout());
    //topPanel.add(new JLabel("URL"), BorderLayout.WEST);
    topPanel.add(topButtons, BorderLayout.WEST);
    topPanel.add(cbox, BorderLayout.CENTER);

    // html Viewer
    htmlViewer = new JEditorPane();
    //htmlViewer.setEditorKit(kit);
    htmlViewer.setContentType("text/html; charset=iso-8859-1");
    //setUrlString("http://www.unidata.ucar.edu/staff/caron/");

    htmlViewer.setEditable(false);
    htmlViewer.addHyperlinkListener( new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        // System.out.println("hyperlinkUpdate event "+e.getEventType()+" "+e.getDescription());
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
          statusText.setText( e.getDescription());

        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
          statusText.setText( "");

        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          JEditorPane pane = (JEditorPane) e.getSource();
          if (showEvent || Debug.isSet("HtmlBrowser/showEvent"))
            System.out.println("HyperlinkEvent= "+e.getURL()+" == "+e.getDescription());

          if (e instanceof HTMLFrameHyperlinkEvent) {
            HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            doc.processHTMLFrameHyperlinkEvent(evt);
          }
          else {
            // a little ad-hoc klugerama
            String desc = e.getDescription();
            if (desc.startsWith("dataset:")) {
              String urlString = desc.substring(8);
              firePropertyChangeEvent( new PropertyChangeEvent(this, "datasetURL", null, urlString));

            } else if (desc.startsWith("catref:")) {
              String urlString = desc.substring(7);
              firePropertyChangeEvent( new PropertyChangeEvent(this, "catrefURL", null, urlString));

            } else
              setURL(e.getURL());
          }
        }
      }
    });

      //status label
    JPanel statusPanel = new JPanel( new BorderLayout());
    statusText = new JLabel("status text");
    statusPanel.add(statusText, BorderLayout.WEST);

   // put it all together
    setLayout(new BorderLayout());
    add(topPanel, BorderLayout.NORTH);
    add( new JScrollPane(htmlViewer), BorderLayout.CENTER);
    add(statusPanel, BorderLayout.SOUTH);
  }

  public void save() {
    cbox.save();
  }

  // event handling

  /**
   * Fires a PropertyChangeEvent:
   * propertyName = "datasetURL", getNewValue() = dataset URL string
   */
  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
  }

  // set new content from outside
  public void setUrlString(String urlString) { addNewPage( new Page( urlString)); }
  public void setURL(URL url) { addNewPage( new Page( url)); }
  public void setContent(String title, String content) { addNewPage( new Page( title, content)); }

    // always add at the end
  private void addNewPage( Page page) {
    // if not at end, chop off extra when new page arrives
    if (currentPage < nav.size()-1) {
      ArrayList nn = new ArrayList();
      for (int i=0; i <= currentPage; i++)
        nn.add( nav.get(i));
      nav = nn;
    }

    nav.add( page);
    currentPage = nav.size()-1;
    showCurrentPage();
    enableActions();
    if (debug) System.out.println("---addNewPage done="+currentPage+" "+page);
  }

  private void incrCurrentPage() {
    if (currentPage < nav.size()-1) {
      currentPage++;
      showCurrentPage();
    }
  }
  private void decrCurrentPage() {
    if (currentPage > 0) {
      currentPage--;
      showCurrentPage();
    }
  }
  private void showCurrentPage() {
    Page p = (Page) nav.get(currentPage);
    p.show();
    if (debug) System.out.println("showCurrentPage current="+currentPage+" "+p);
    enableActions();

    // add to the combobox; if already exists, bring to top of combo box
    eventsOK = false;
    cbox.addItem( p);
    eventsOK = true;
  }
  // make this page the current one, dont change stack
  private void setCurrentPage(Page page) {
    int i = nav.indexOf(page);
    if (i < 0) return;
    if (debug) System.out.println("setCurrentPage to="+i);
    currentPage = i;
    showCurrentPage();
  }
  private void enableActions() {
    backAction.setEnabled( currentPage > 0);
    forwardAction.setEnabled( currentPage < nav.size()-1);
  }

  private class Page {
    String type;
    String title;
    String content;
    URL url;
    Page(String urlString) {
      this.title = urlString;
      this.type = "urlString";
    }
    Page(URL url) {
      this.url = url;
      this.type = "url";
    }
    Page(String title, String content) {
      this.title = title;
      this.content = content;
      this.type = "content";
    }

    void show() {
      // htmlViewer.setDocument(new HTMLDocument()); // CHANGED
      htmlViewer.setContentType("text/html; charset=iso-8859-1");
      htmlViewer.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
      htmlViewer.getDocument().putProperty("stream", null);
      //htmlViewer.setEditable(false);

      if (debugDoc) {
        System.out.println("--show page starts with content type = "+htmlViewer.getContentType()+
          " kit="+htmlViewer.getEditorKit());
        HTMLEditorKit hkit = (HTMLEditorKit) htmlViewer.getEditorKit();
        System.out.println("   Css="+hkit.getStyleSheet().hashCode());

        if (htmlViewer.getDocument() instanceof HTMLDocument)
          showDoc( (HTMLDocument) htmlViewer.getDocument());
      }

      //htmlViewer.setEditorKit(kit);
      try {
        switch (type) {
          case "urlString":
            htmlViewer.setPage(title);
            break;
          case "url":
            htmlViewer.setPage(url.toString());
            break;
          case "content":
            htmlViewer.setText(content);
            break;
        }
        htmlViewer.setCaretPosition(0);

        if (debugDoc) {
          System.out.println(" show page ends with content type = " +
                             htmlViewer.getContentType() +
                             " kit=" + htmlViewer.getEditorKit());
        HTMLEditorKit hkit = (HTMLEditorKit) htmlViewer.getEditorKit();
        System.out.println("   Css="+hkit.getStyleSheet().hashCode());
        if (htmlViewer.getDocument() instanceof HTMLDocument)
            showDoc( (HTMLDocument) htmlViewer.getDocument());
          htmlViewer.repaint();
        }
      }
      catch (IOException ioe) {
        statusText.setText("Error: " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

    public String toString() {
      if (title != null) return title;
      if (url != null) return url.toString();
      return "huh?";
    }

    void showDoc( HTMLDocument doc) {
      System.out.println(" Doc base="+doc.getBase()); // +" ss="+doc.getStyleSheet());

      Dictionary dict = doc.getDocumentProperties();
      Enumeration e = dict.keys();
      System.out.println(" DocumentProperties");
      while (e.hasMoreElements()) {
        Object key = e.nextElement();
        System.out.println("  key= "+key+" value="+dict.get(key));
      }
    }
  }

 ////////////////////////////////////////////////////////////////////////////////////////////
 // LOOK Factor this out
 /** Wrap this in a JDialog component.
   *
   * @param parent      JFrame (application) or JApplet (applet) or null
   * @param title       dialog window title
   * @param modal     is modal
   */
  public JDialog makeDialog( RootPaneContainer parent, String title, boolean modal) {
    this.parent = parent;
    return new Dialog( parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( HtmlBrowser.Dialog.this);
        }
      });

      // add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      topButtons.add(dismissButton, null);

      dismissButton.addActionListener(e -> setVisible(false));

     // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add( HtmlBrowser.this, BorderLayout.CENTER);
      pack();
    }
  }

}
