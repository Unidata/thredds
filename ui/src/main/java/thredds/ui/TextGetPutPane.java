// $Id: TextGetPutPane.java 50 2006-07-12 16:30:06Z caron $
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

import thredds.catalog.*;
import thredds.catalog.query.*;

import ucar.util.prefs.*;
import ucar.nc2.util.net.HttpClientManager;
import ucar.nc2.util.IO;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 * A text widget that does get and put to a web URL.
 *
 * @author John Caron
 * @version $Id: TextGetPutPane.java 50 2006-07-12 16:30:06Z caron $
 */

public class TextGetPutPane extends TextHistoryPane {
    private PreferencesExt prefs;
    private JComboBox cb;
    private JPanel buttPanel;

  private boolean addFileButton = true;
    private AbstractAction fileAction = null;
    private FileManager fileChooserReader;

    private GetContentsTask task;
    //private HttpSession httpSession;

    public TextGetPutPane(PreferencesExt prefs) {
      super(true);

      this.prefs = prefs;
      /* ta = new JTextArea();
      ta.setFont( new Font("Monospaced", Font.PLAIN, 12)); */

      // combo box holds a list of urls
      cb = new JComboBox();
      cb.setEditable(true);
      if (prefs != null)
        setList((ArrayList) prefs.getBean("list", null));

      if (addFileButton) {
        fileChooserReader = new FileManager(null, null, "xml", "THREDDS catalogs",
            (prefs == null) ? null : (PreferencesExt) prefs.node("fileChooserReader"));
        fileAction =  new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String filename = fileChooserReader.chooseFilename();
          if (filename == null) return;
          cb.setSelectedItem("file:"+filename);
          }
        };
        BAMutil.setActionProperties( fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
      }

      JButton getButton = new JButton("Get");
      getButton.setToolTipText("GET URL contents");
      getButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setURL( (String) cb.getSelectedItem());
        }
      });
      JButton validButton = new JButton("Validate");
      validButton.setToolTipText("Validate catalog");
      validButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          validate( (String) cb.getSelectedItem());
        }
      });
      JButton putButton = new JButton("Put");
      putButton.setToolTipText("PUT URL contents");
      putButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            putURL( (String) cb.getSelectedItem());
          } catch (IOException e1) {
            javax.swing.JOptionPane.showMessageDialog(null, e1.getMessage());
          }
          firePutActionEvent();
        }
      });

      /* AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clear();
          if (httpSession != null)
            setText( httpSession.getInfo());
        }
      }); */

      buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      if (null != fileAction)
        BAMutil.addActionToContainer(buttPanel, fileAction);
      buttPanel.add( getButton);
      buttPanel.add( validButton);
      buttPanel.add(putButton);
      //buttPanel.add( infoButton);

      JPanel topPanel = new JPanel( new BorderLayout());
      topPanel.add(new JLabel("URL:"), BorderLayout.WEST);
      topPanel.add(cb, BorderLayout.CENTER);
      topPanel.add(buttPanel, BorderLayout.EAST);

      // setLayout( new BorderLayout());
      add( topPanel, BorderLayout.NORTH);
      // add( new JScrollPane(ta), BorderLayout.CENTER);
    }

    public void addButton( Component c) { buttPanel.add(c); }

    private EventListenerList listenerList = new EventListenerList();
    public void addPutActionListener(  ActionListener listener) {
      listenerList.add( ActionListener.class, listener);
    }
    public void removePutActionListener( ActionListener listener) {
      listenerList.remove(ActionListener.class, listener);
    }

    private void firePutActionEvent() {
      ActionEvent event = new ActionEvent(this, 0, "Put");
      // Process the listeners last to first
      Object[] listeners = listenerList.getListenerList();
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == ActionListener.class) {
          ((ActionListener)listeners[i+1]).actionPerformed(event);
        }
      }
    }

    public void setURL(String urlString) {
      if (urlString == null) return;
      if (urlString.startsWith("file:")) {
        urlString = urlString.substring(5);
        String contents;
        try {
          contents = IO.readFile( urlString);
        } catch (IOException e) {
          contents = e.getMessage();
        }
        ta.setText( contents);
        return;
      }

      // httpSession = HttpSession.getSession();

      task = new GetContentsTask(urlString);
      thredds.ui.ProgressMonitor pm = new thredds.ui.ProgressMonitor(task);
      pm.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // System.out.println(" setURL event"+e.getActionCommand());
          if (e.getActionCommand().equals("success")) {
            ta.setText(task.contents);

            // add to combobox
            ArrayList list = getList();
            if (!list.contains(task.urlString))
              cb.addItem(task.urlString);
            cb.setSelectedItem(task.urlString);
          }
        }
      });
      pm.start(this, "Open URL " + urlString, 10);
    }

    public void setCatalog(String urlString, InvCatalogImpl cat) throws IOException {
      // add URL to combobox
      ArrayList list = getList();
      if (!list.contains(urlString))
        cb.addItem(urlString);
      cb.setSelectedItem(urlString);

      // write catalog to text
      ByteArrayOutputStream os = new ByteArrayOutputStream(20000);
      cat.writeXML(os, true);
      ta.setText(os.toString());
    }

    private InvCatalogFactory catFactory = null;
    private DqcFactory dqcFactory = null;
    void validate(String urlString) {
      if (urlString == null) return;
      URI uri;
      try {
        uri = new URI(urlString);
      }
      catch (URISyntaxException e) {
        javax.swing.JOptionPane.showMessageDialog(null, "URISyntaxException on URL (" +
            urlString + ") " + e.getMessage() + "\n");
        return;
      }
      String contents = getText();
      boolean isCatalog = contents.indexOf("queryCapability") < 0;

      ByteArrayInputStream is = new ByteArrayInputStream(contents.getBytes());

      if (isCatalog) {
        if (catFactory == null) catFactory = InvCatalogFactory.getDefaultFactory(true);
        InvCatalogImpl catalog = catFactory.readXML(is, uri);
        StringBuilder buff = new StringBuilder();
        boolean check = catalog.check(buff);
        javax.swing.JOptionPane.showMessageDialog(this,
           "Catalog Validation = " + check + "\n" +  buff.toString());

      }  else {
        try {
          if (dqcFactory == null)
            dqcFactory = new DqcFactory(true);
          QueryCapability dqc = dqcFactory.readXML(is, uri);
          javax.swing.JOptionPane.showMessageDialog(this,
             "DQC Errors = \n" +dqc.getErrorMessages());
        }
        catch (IOException ioe) {
          javax.swing.JOptionPane.showMessageDialog(this,
             "IO Error = " +ioe);
        }
      }
    }

    void putURL(String uriString) throws IOException {
      if (uriString == null) return;
      URI uri;
      try {
        uri = new URI( uriString);
      } catch (URISyntaxException e) {
        System.out.println("** TextGetPutPane URISyntaxException="+uriString);
        return;
      }

      String contents = ta.getText();
      String s = uri.getScheme();
      if (s.equalsIgnoreCase("file")) {
        String path = uri.getPath();
        if (path.startsWith("/")) path = path.substring(1);
        IO.writeToFile( contents, path);

      } else {

        int status = HttpClientManager.putContent(uriString, contents);
        javax.swing.JOptionPane.showMessageDialog(this, "Status code= " + status);
      }
    }

    void setList(ArrayList list) {
      if (list == null) return;
      cb.removeAllItems();
      for (int i=0; i< list.size(); i++)
        cb.addItem( list.get(i));
      cb.revalidate();
    }

    ArrayList getList() {
      ArrayList list = new ArrayList();
      for (int i=0; i< cb.getItemCount(); i++)
        list.add( cb.getItemAt(i));
      return list;
    }

    public void save() {
      if (prefs != null)
        prefs.putBeanObject("list", getList());
    }

    public void clear() {
      ta.setText(null);
    }

    public String getText() {
      return ta.getText();
    }

    public void gotoTop() {
      ta.setCaretPosition(0);
    }

    public void setText(String text) {
      ta.setText(text);
    }

  private class GetContentsTask extends ProgressMonitorTask {
    String urlString;
    String contents;

    GetContentsTask( String urlString) {
      this.urlString = urlString;
    }

    public void run() {
      try {
        contents = HttpClientManager.getContent(urlString);

      } catch (Exception e) {
        setError(e.getMessage());
        done = true;
        return;
      }

      success = !cancel;
      done = true;
    }
  }


}