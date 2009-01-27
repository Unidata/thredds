// $Id: HtmlBrowser.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.ui;

import ucar.util.prefs.ui.ComboBox;
import ucar.util.prefs.ui.Debug;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * A simple HTML Browser based on JEditPane.
 *
 * @author John Caron
 * @version $Id: HtmlBrowser.java 50 2006-07-12 16:30:06Z caron $
 */

public class HtmlBrowser extends JPanel {
  static private EditorKit kit = JEditorPane.createEditorKitForContentType("text/html");
  private ucar.util.prefs.PreferencesExt prefs;
  private RootPaneContainer parent = null;

  private EventListenerList listenerList = new EventListenerList();
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
   * Add a PropertyChangeEvent Listener. Throws a PropertyChangeEvent:
   *   propertyName = "datasetURL", getNewValue() = dataset URL string
   */
  public void addPropertyChangeListener( PropertyChangeListener l) {
    listenerList.add(PropertyChangeListener.class, l);
  }

  /**
   * Remove a PropertyChangeEvent Listener.
   */
  public void removePropertyChangeListener( PropertyChangeListener l) {
    listenerList.remove(PropertyChangeListener.class, l);
  }

  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    // System.out.println("firePropertyChangeEvent "+event);

    // Process the listeners last to first
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == PropertyChangeListener.class) {
        ((PropertyChangeListener)listeners[i+1]).propertyChange(event);
      }
    }
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
        if (type.equals("urlString"))
          htmlViewer.setPage(title);
        else if (type.equals("url"))
          htmlViewer.setPage(url.toString());
        else if (type.equals("content"))
          htmlViewer.setText(content);
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

      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      });

     // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add( HtmlBrowser.this, BorderLayout.CENTER);
      pack();
    }
  }

}

/* Change History:
   $Log: HtmlBrowser.java,v $
   Revision 1.9  2004/10/15 19:16:07  caron
   enum now keyword in 1.5
   SelectDateRange send ISO date string

   Revision 1.8  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.7  2004/09/24 03:26:33  caron
   merge nj22

   Revision 1.6  2004/06/12 02:07:56  caron
   special URLs have dataset, catref

   Revision 1.5  2004/05/21 05:57:34  caron
   release 2.0b

   Revision 1.4  2004/05/11 23:30:35  caron
   release 2.0a

   Revision 1.3  2004/03/05 23:43:24  caron
   1.3.1 release

   Revision 1.2  2004/02/20 05:02:53  caron
   release 1.3

   Revision 1.1  2003/12/04 22:27:47  caron
   *** empty log message ***

 */