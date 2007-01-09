// $Id: URLDumpPane.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.ui;

import thredds.util.IO;
import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;
import javax.swing.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.*;


/**
 * A text widget to dump a web URL.
 *
 * @author John Caron
 * @version $Id: URLDumpPane.java 50 2006-07-12 16:30:06Z caron $
 */

public class URLDumpPane extends TextHistoryPane {
  private static final int GET = 0;
  private static final int PUT = 1;
  private static final int HEAD = 2;
  private static final int OPTIONS = 3;

    private PreferencesExt prefs;
    private ComboBox cb;
    private GetContentsTask task;
    private HttpURLConnection currentConnection = null;

    public URLDumpPane(PreferencesExt prefs) {
      super(true);
      this.prefs = prefs;

      // combo box holds a list of urls
      cb = new ComboBox( prefs);

      JButton headButton = new JButton("HEAD");
      headButton.setToolTipText("Open URL connection, HEADERS only");
      headButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL( urlString, "HEAD");
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton headButton2 = new JButton("Head2");
      headButton2.setToolTipText("Open URL connection, HEADERS only");
      headButton2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, HEAD);
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton readButton = new JButton("GET");
      readButton.setToolTipText("Open URL connection, show content");
      readButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          readURL( urlString);
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton read2Button = new JButton("Get2");
      read2Button.setToolTipText("Open URL connection, show content using HttpClient");
      read2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, GET);
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton optButton = new JButton("Options");
      optButton.setToolTipText("Get server options");
      optButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL( urlString, "OPTIONS");
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton opt2Button = new JButton("Options2");
      opt2Button.setToolTipText("Get server options");
      opt2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, OPTIONS);
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton putButton = new JButton("PUT");
      putButton.setToolTipText("Send PUT command");
      putButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL( urlString, "PUT");
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton put2Button = new JButton("Put2");
      put2Button.setToolTipText("Put (HttpCLient)");
      put2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, PUT);
          gotoTop();
          cb.addItem( urlString);
        }
      });


      JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      buttPanel.add( headButton);
      buttPanel.add( headButton2);
      buttPanel.add( readButton);
      buttPanel.add( read2Button);
      buttPanel.add( optButton);
      buttPanel.add( opt2Button);
      buttPanel.add( putButton);
      buttPanel.add( put2Button);

      JPanel topPanel = new JPanel( new BorderLayout());
      topPanel.add(new JLabel("URL:"), BorderLayout.WEST);
      topPanel.add(cb, BorderLayout.CENTER);
      topPanel.add(buttPanel, BorderLayout.EAST);

      // setLayout( new BorderLayout());
      add( topPanel, BorderLayout.NORTH);
      // add( new JScrollPane(ta), BorderLayout.CENTER);
    }

  ///////////////////////////////////////////////////////
  // Uses apache commons HttpClient

  private void openURL2(String urlString, int cmd) {
    clear();

    //Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));
    HttpClient httpclient = new HttpClient();

    httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, new UrlAuthenticatorDialog( null));

    HttpMethodBase m = null;
    if (cmd == GET)
      m = new GetMethod(urlString);
    else if (cmd == HEAD)
      m = new HeadMethod(urlString);
    else if (cmd == OPTIONS)
      m = new OptionsMethod(urlString);
    else if (cmd == PUT) {
      PutMethod p = new PutMethod(urlString);
      p.setRequestBody( ta.getText());
      m = p;
    }

    appendLine("HttpClient "+m.getName()+" "+urlString);
    appendLine("   do Authentication= "+m.getDoAuthentication());
    appendLine("   follow Redirects= "+m.getFollowRedirects());

    HttpMethodParams p = m.getParams();
    appendLine("   cookie policy= "+p.getCookiePolicy());
    appendLine("   http version= "+p.getVersion().toString());
    appendLine("   timeout (msecs)= "+p.getSoTimeout());
    appendLine("   virtual host= "+p.getVirtualHost());

    printHeaders("Request Headers = ", m.getRequestHeaders());
    appendLine(" ");

    try {
      httpclient.executeMethod(m);

      printHeaders("Request Headers2 = ", m.getRequestHeaders());
      appendLine(" ");

      appendLine("Status = "+m.getStatusCode()+" "+m.getStatusText());
      appendLine("Status Line = "+m.getStatusLine());
      printHeaders("Response Headers = ", m.getResponseHeaders());
      if (cmd == GET) {
        appendLine("ResponseBody---------------");
        appendLine(m.getResponseBodyAsString());
      } else if (cmd == OPTIONS)
        printEnum("AllowedMethods = ", ((OptionsMethod)m).getAllowedMethods());

      printHeaders("Response Footers = ", m.getResponseFooters());

    } catch (IOException e) {
      e.printStackTrace();
      appendLine("IOException = "+e.getMessage());

    } finally {
      m.releaseConnection();
    }

  }

  private void printHeaders(String title, Header[] heads) {
    appendLine(title);
    for (int i = 0; i < heads.length; i++) {
      Header head = heads[i];
      append("  "+head.toString());
    }
  }

  private void printEnum(String title, Enumeration en) {
    appendLine(title);
    while (en.hasMoreElements()) {
      Object o =  en.nextElement();
      append("  "+o.toString());
    }
    appendLine("");
  }

  ///////////////////////////////////////////////////////
  // Uses java.net

  private void openURL(String urlString, String command) {
    try {
      //Open the URLConnection for reading
      URL u = new URL(urlString);
      currentConnection = (HttpURLConnection) u.openConnection();
      currentConnection.setRequestMethod(command);
      currentConnection.setAllowUserInteraction(true);

      clear();
      appendLine(command+" request for "+urlString);

      // request headers
      Map reqs = currentConnection.getRequestProperties();
      Iterator reqIter = reqs.keySet().iterator();
      while (reqIter.hasNext()) {
        String key = (String) reqIter.next();
        java.util.List values = (java.util.List) reqs.get( key);
        append(" "+key + ": ");
        for (int i = 0; i < values.size(); i++) {
          String v =  (String) values.get(i);
          append(v+" ");
        }
        appendLine("");
      }
      appendLine("");
      appendLine("getFollowRedirects="+currentConnection.getFollowRedirects());
      appendLine("getInstanceFollowRedirects="+currentConnection.getInstanceFollowRedirects());
      appendLine("AllowUserInteraction="+currentConnection.getAllowUserInteraction());
      appendLine("");

      int code = currentConnection.getResponseCode();
      String response = currentConnection.getResponseMessage();

      // response headers
      appendLine(" HTTP/1.x " + code + " " + response);
      for (int j = 1; ; j++) {
        String header = currentConnection.getHeaderField(j);
        String key = currentConnection.getHeaderFieldKey(j);
        if (header == null || key == null) break;
        appendLine(" "+key + ": " + header);
      }

      appendLine("");
      appendLine("contents:");

      // read it
      java.io.InputStream is = currentConnection.getInputStream();
      ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
      thredds.util.IO.copy(is, bout);
      is.close();

      append(new String(bout.toByteArray()));
      appendLine("end contents");

    } catch (MalformedURLException e) {
      append(urlString + " is not a parseable URL");
    }
    catch (IOException e) {
      e.printStackTrace();
      System.err.println(e);
    }
  }

  private void readURL(String urlString) {
    try {
      //Open the URLConnection for reading
      URL u = new URL(urlString);
      currentConnection = (HttpURLConnection) u.openConnection();
      //uc.setAllowUserInteraction(true);

      clear();
      appendLine("GET request for "+urlString);

      // request headers
      Map reqs = currentConnection.getRequestProperties();
      Iterator reqIter = reqs.keySet().iterator();
      while (reqIter.hasNext()) {
        String key = (String) reqIter.next();
        java.util.List values = (java.util.List) reqs.get( key);
        append(" "+key + ": ");
        for (int i = 0; i < values.size(); i++) {
          String v =  (String) values.get(i);
          append(v+" ");
        }
        appendLine("");
      }
      appendLine("");

      int code = currentConnection.getResponseCode();
      String response = currentConnection.getResponseMessage();

      // result headers
      appendLine(" HTTP/1.x " + code + " " + response);
      for (int j = 1; ; j++) {
        String header = currentConnection.getHeaderField(j);
        String key = currentConnection.getHeaderFieldKey(j);
        if (header == null || key == null) break;
        appendLine(" "+key + ": " + header);
      }

      appendLine("");

      // read it
      java.io.InputStream is = currentConnection.getInputStream();
      ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
      thredds.util.IO.copy(is, bout);
      is.close();

      append(new String(bout.toByteArray()));

    } catch (MalformedURLException e) {
      append(urlString + " is not a parseable URL");
    }
    catch (IOException e) {
      e.printStackTrace();
      System.err.println(e);
      appendLine(e.getMessage());
    }
  }

    public void setURL(String urlString) {
      if (urlString == null) return;

      task = new GetContentsTask(urlString);
      thredds.ui.ProgressMonitor pm = new thredds.ui.ProgressMonitor(task);
      pm.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // System.out.println(" setURL event"+e.getActionCommand());
          if (e.getActionCommand().equals("success")) {
            ta.setText(task.contents);
          }

        }
      });
      pm.start(this, "Open URL " + urlString, 10);
      return;
    }

    void putURL(String urlString) {
      if (urlString == null) return;
      String contents = ta.getText();
      IO.Result result = thredds.util.IO.putToURL( urlString, contents);
      javax.swing.JOptionPane.showMessageDialog(this, "Status code= "+result.code+"\n"+result.message);
    }

    public void save() {
       cb.save();
    }

    public void clear() {ta.setText(null); }
    public void gotoTop() { ta.setCaretPosition(0); }
    public void setText(String text) { ta.setText(text); }
    public void append(String text) { ta.append(text); }
    public void appendLine(String text) {
      ta.append(text);
      ta.append("\n");
    }

  /* public class ConsoleAuthPrompter implements CredentialsProvider {

    private BufferedReader in = null;

    public ConsoleAuthPrompter() {
      super();
      this.in = new BufferedReader(new InputStreamReader(System.in));
    }

    private String readConsole() throws IOException {
      return this.in.readLine();
    }

    public Credentials getCredentials(final AuthScheme authscheme, final String host, int port, boolean proxy)
            throws CredentialsNotAvailableException {

      if (authscheme == null) {
        return null;
      }

      System.out.println("getCredentials AuthScheme="+authscheme.getClass().getName());
      System.out.println("  authscheme="+authscheme.getSchemeName()+" realm="+authscheme.getRealm()+" connect="+authscheme.isConnectionBased());
      System.out.println("  host="+host+" port= "+port +"proxy="+proxy);

      try {
        if (authscheme instanceof NTLMScheme) {
          System.out.println(host + ":" + port + " requires Windows authentication");
          System.out.print("Enter domain: ");
          String domain = readConsole();
          System.out.print("Enter username: ");
          String user = readConsole();
          System.out.print("Enter password: ");
          String password = readConsole();
          return new NTCredentials(user, password, host, domain);
        } else if (authscheme instanceof RFC2617Scheme) {
          System.out.println(host + ":" + port + " requires authentication with the realm '"
                  + authscheme.getRealm() + "'");
          System.out.print("Enter username: ");
          String user = readConsole();
          System.out.print("Enter password: ");
          String password = readConsole();
          return new UsernamePasswordCredentials(user, password);
        } else {
          throw new CredentialsNotAvailableException("Unsupported authentication scheme: " +
                  authscheme.getSchemeName());
        }
      } catch (IOException e) {
        throw new CredentialsNotAvailableException(e.getMessage(), e);
      }
    }
  } */

  private class GetContentsTask extends ProgressMonitorTask {
    String urlString;
    String contents;

    GetContentsTask( String urlString) {
      this.urlString = urlString;
    }

    public void run() {
      try {
        contents = thredds.util.IO.readURLcontentsWithException(urlString);
      } catch (IOException e) {
        setError(e.getMessage());
        done = true;
        return;
      }

      success = !cancel;
      done = true;
    }
  }

  private static XMLStore xstore;
  private static URLDumpPane main;
  public static void main(String args[]) throws Exception {
    JFrame frame = new JFrame("URL Dump Pane");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          main.save();
          xstore.save();
        } catch (IOException ioe) {}
        System.exit(0);
      }
    });

    java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));

    // open up the preferences file(s)
    try {
      String storeFilename = XMLStore.makeStandardFilename(".unidata", "URLDumpPane.xml");
      xstore = XMLStore.createFromFile(storeFilename, null);
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    PreferencesExt store = xstore.getPreferences();

    main = new URLDumpPane(store);

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(200, 200);
    frame.setSize(900, 700);
    frame.setVisible(true);
  }

}