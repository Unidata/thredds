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

import ucar.nc2.util.IO;
import ucar.nc2.util.net.HttpClientManager;
import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.swing.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.*;


/**
 * A text widget to dump a web URL.
 * Uses java.net.HttpURLConnection or org.apache.commons.httpclient.
 *
 * @author John Caron
 * @see thredds.ui.UrlAuthenticatorDialog
 */

public class URLDumpPane extends TextHistoryPane {
  private static final int GET = 0;
  private static final int PUT = 1;
  private static final int HEAD = 2;
  private static final int OPTIONS = 3;

    private ComboBox cb;
    private GetContentsTask task;
    private HttpURLConnection currentConnection = null;

    public URLDumpPane(PreferencesExt prefs) {
      super(true);

      // combo box holds a list of urls
      cb = new ComboBox( prefs);


      JButton headButton2 = new JButton("Head");
      headButton2.setToolTipText("Open URL connection, Headers only using HttpClient");
      headButton2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, HEAD);
          gotoTop();
          cb.addItem( urlString);
        }
      });


      JButton read2Button = new JButton("Get");
      read2Button.setToolTipText("Open URL connection, Get content using HttpClient");
      read2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, GET);
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton opt2Button = new JButton("Options");
      opt2Button.setToolTipText("Server options using HttpClient");
      opt2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, OPTIONS);
          gotoTop();
          cb.addItem( urlString);
        }
      });


      JButton put2Button = new JButton("Put");
      put2Button.setToolTipText("Put using HttpClient");
      put2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL2( urlString, PUT);
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JButton headButton = new JButton("HEAD");
      headButton.setToolTipText("Open URL connection, HEADERS only using HttpURLConnection");
      headButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL( urlString, "HEAD");
          gotoTop();
          cb.addItem( urlString);
        }
      });


      JButton readButton = new JButton("GET");
      readButton.setToolTipText("Open URL connection, show content using HttpURLConnection");
      readButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          readURL( urlString);
          gotoTop();
          cb.addItem( urlString);
        }
      });


      JButton optButton = new JButton("OPTIONS");
      optButton.setToolTipText("Get server options using HttpURLConnection");
      optButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL( urlString, "OPTIONS");
          gotoTop();
          cb.addItem( urlString);
        }
      });


      JButton putButton = new JButton("PUT");
      putButton.setToolTipText("Send PUT command using HttpURLConnection");
      putButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String urlString = (String) cb.getSelectedItem();
          openURL( urlString, "PUT");
          gotoTop();
          cb.addItem( urlString);
        }
      });

      JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      buttPanel.add( headButton2);
      buttPanel.add( read2Button);
      buttPanel.add( opt2Button);
      buttPanel.add( putButton);
      buttPanel.add( put2Button);
      buttPanel.add( headButton);
      buttPanel.add( readButton);
      buttPanel.add( optButton);
      buttPanel.add( putButton);

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

    HttpClient httpclient = HttpClientManager.getHttpClient();
    HttpMethodBase m = null;
    if (cmd == GET)
      m = new GetMethod(urlString);
    else if (cmd == HEAD)
      m = new HeadMethod(urlString);
    else if (cmd == OPTIONS)
      m = new OptionsMethod(urlString);
    else if (cmd == PUT) {
      PutMethod p = new PutMethod(urlString);
      try {
        p.setRequestEntity( new StringRequestEntity( ta.getText(), "application/text", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        e.printStackTrace(new PrintStream(bos));
        appendLine(bos.toString());
        return;
      }
      m = p;
    }

    m.setRequestHeader("Accept-Encoding","gzip,deflate");

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
        appendLine("\nResponseBody---------------");

        String charset = m.getResponseCharSet();
        if (charset == null) charset = "UTF-8";
        String contents = null;       

        // check for deflate and gzip compression
        Header h = m.getResponseHeader("content-encoding");
        String encoding = (h == null) ? null : h.getValue();

        if (encoding != null && encoding.equals("deflate")) {
          byte[] body = m.getResponseBody();
          InputStream is = new BufferedInputStream(new InflaterInputStream( new ByteArrayInputStream(body)), 10000);
          contents = IO.readContents(is, charset);
          double ratio = (double) contents.length() / body.length;
          appendLine("  deflate encoded="+body.length+" decoded="+contents.length()+" ratio= "+ratio);

        } else if (encoding != null && encoding.equals("gzip")) {
          byte[] body = m.getResponseBody();
          InputStream is = new BufferedInputStream(new GZIPInputStream( new ByteArrayInputStream(body)),  10000);
          contents = IO.readContents(is, charset);
          double ratio = (double) contents.length() / body.length;
          appendLine("  gzip encoded="+body.length+" decoded="+contents.length()+" ratio= "+ratio);

        } else {
          byte[] body = m.getResponseBody(50 * 1000);
          contents = new String(body, charset);
        }

        appendLine(contents);

      } else if (cmd == OPTIONS)
        printEnum("AllowedMethods = ", ((OptionsMethod)m).getAllowedMethods());

      printHeaders("Response Footers = ", m.getResponseFooters());

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
      e.printStackTrace(new PrintStream(bos));
      appendLine(bos.toString());

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
      Map<String, List<String>> reqs = currentConnection.getRequestProperties();
      for (String key : reqs.keySet()) {
        append(" " + key + ": ");
        for (String v : reqs.get(key))
          append(v + " ");
        appendLine("");
      }
      appendLine("");
      appendLine("getFollowRedirects="+HttpURLConnection.getFollowRedirects());
      appendLine("getInstanceFollowRedirects="+currentConnection.getInstanceFollowRedirects());
      appendLine("AllowUserInteraction="+currentConnection.getAllowUserInteraction());
      appendLine("");

      int code = currentConnection.getResponseCode();
      String response = currentConnection.getResponseMessage();

      // response headers
      appendLine(" HTTP/1.x " + code + " " + response);
      appendLine(" content-length: " + currentConnection.getContentLength());
      appendLine(" content-encoding: " + currentConnection.getContentEncoding());
      appendLine(" content-type: " + currentConnection.getContentType());
      appendLine("\nHeaders: ");

      for (int j = 1; true; j++) {
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
      IO.copy(is, bout);
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
      Map<String, List<String>> reqs = currentConnection.getRequestProperties();
      for (String key : reqs.keySet()) {
        append(" " + key + ": ");
        for (String v : reqs.get(key))
          append(v + " ");
        appendLine("");
      }
      appendLine("");

      int code = currentConnection.getResponseCode();
      String response = currentConnection.getResponseMessage();

      // result headers
      appendLine(" HTTP/1.x " + code + " " + response);
      for (int j = 1; true; j++) {
        String header = currentConnection.getHeaderField(j);
        String key = currentConnection.getHeaderFieldKey(j);
        if (header == null || key == null) break;
        appendLine(" "+key + ": " + header);
      }

      appendLine("");

      // read it
      java.io.InputStream is = currentConnection.getInputStream();
      ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
      IO.copy(is, bout);
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
      IO.HttpResult result = IO.putToURL( urlString, contents);
      javax.swing.JOptionPane.showMessageDialog(this, "Status code= "+result.statusCode +"\n"+result.message);
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
        contents = IO.readURLcontentsWithException(urlString);
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