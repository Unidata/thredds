/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.widget;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.tools.CatalogXmlWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.IO;
import ucar.nc2.util.net.HttpClientManager;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.ui.widget.ProgressMonitor;
import ucar.ui.widget.ProgressMonitorTask;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

//import thredds.catalog.query.*;

/**
 * A text widget that does get and put to a web URL.
 *
 * @author John Caron
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
        BAMutil
            .setActionProperties( fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
      }

      JButton getButton = new JButton("Get");
      getButton.setToolTipText("GET URL contents");
      getButton.addActionListener(e -> setURL( (String) cb.getSelectedItem()));
      JButton validButton = new JButton("Validate");
      validButton.setToolTipText("Validate catalog");
      validButton.addActionListener(e -> validate( (String) cb.getSelectedItem()));
      JButton putButton = new JButton("Put");
      putButton.setToolTipText("PUT URL contents");
      putButton.addActionListener(e -> {
          try {
            putURL( (String) cb.getSelectedItem());
          } catch (IOException e1) {
            javax.swing.JOptionPane.showMessageDialog(null, e1.getMessage());
          }
          firePutActionEvent();
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
      ProgressMonitor pm = new ProgressMonitor(task);
      pm.addActionListener(e -> {
          // System.out.println(" setURL event"+e.getActionCommand());
          if (e.getActionCommand().equals("success")) {
            ta.setText(task.contents);

            // add to combobox
            ArrayList list = getList();
            if (!list.contains(task.urlString))
              cb.addItem(task.urlString);
            cb.setSelectedItem(task.urlString);
          }
      });
      pm.start(this, "Open URL " + urlString, 10);
    }

    public void setCatalog(String urlString, Catalog cat) throws IOException {
      // add URL to combobox
      ArrayList list = getList();
      if (!list.contains(urlString))
        cb.addItem(urlString);
      cb.setSelectedItem(urlString);

      // write catalog to text
      ByteArrayOutputStream os = new ByteArrayOutputStream(20000);
      CatalogXmlWriter writer = new CatalogXmlWriter();
      writer.writeXML(cat, os, false);
      ta.setText(os.toString(CDM.UTF8));
    }

   //private DqcFactory dqcFactory = null;
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
      //boolean isCatalog = contents.indexOf("queryCapability") < 0;

      ByteArrayInputStream is = new ByteArrayInputStream(contents.getBytes(CDM.utf8Charset));

      try {
        CatalogBuilder catFactory = new CatalogBuilder();
        Catalog cat = catFactory.buildFromLocation(urlString, null);
        boolean isValid = !catFactory.hasFatalError();

       javax.swing.JOptionPane.showMessageDialog(this,
          "Catalog Validation = " + isValid + "\n" +  catFactory.getErrorMessage());

      } catch (IOException e) {
        e.printStackTrace();
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
      for (Object o : list) {
        cb.addItem(o);
      }
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

  private static class GetContentsTask extends ProgressMonitorTask {
    String urlString;
    String contents;

    GetContentsTask( String urlString) {
      this.urlString = urlString;
    }

    public void run() {
      try {
        contents = HttpClientManager.getContentAsString(null, urlString);

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
