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
import ucar.nc2.util.URLnaming;
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
  private enum Library {Commons, java};
  private enum Command {GET, PUT, HEAD, OPTIONS};

  private ComboBox cb;
  private JComboBox implCB;
  private HttpURLConnection currentConnection = null;

  public URLDumpPane(PreferencesExt prefs) {
    super(true);

    // combo box holds a list of urls
    cb = new ComboBox(prefs);

    // holds Library impl enum
    implCB = new JComboBox();
    for (Library e: Library.values())
      implCB.addItem(e);

    JButton buttHead = new JButton("Head");
    buttHead.setToolTipText("Open URL connection, Headers only");
    buttHead.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String urlString = (String) cb.getSelectedItem();
        process(urlString, Command.HEAD);
        gotoTop();
        cb.addItem(urlString);
      }
    });


    JButton buttRead = new JButton("Get");
    buttRead.setToolTipText("Open URL connection, Get content");
    buttRead.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String urlString = (String) cb.getSelectedItem();
        process(urlString, Command.GET);
        gotoTop();
        cb.addItem(urlString);
      }
    });

    JButton buttOpt = new JButton("Options");
    buttOpt.setToolTipText("Server options using HttpClient");
    buttOpt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String urlString = (String) cb.getSelectedItem();
        process(urlString, Command.OPTIONS);
        gotoTop();
        cb.addItem(urlString);
      }
    });


    JButton buttPut = new JButton("Put");
    buttPut.setToolTipText("Put using HttpClient");
    buttPut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String urlString = (String) cb.getSelectedItem();
        process(urlString, Command.PUT);
        gotoTop();
        cb.addItem(urlString);
      }
    });

    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    buttPanel.add(implCB);
    buttPanel.add(buttHead);
    buttPanel.add(buttRead);
    buttPanel.add(buttOpt);
    buttPanel.add(buttPut);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("URL:"), BorderLayout.WEST);
    topPanel.add(cb, BorderLayout.CENTER);
    topPanel.add(buttPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);
  }

  private void process(String urlString, Command cmd) {
    clear();

    Library impl = (Library) implCB.getSelectedItem();
    //if (impl == Library.HttpClient) {
       // openClient(urlString, cmd);
    // } else 
    if (impl == Library.Commons) {
       openURL2(urlString, cmd);
    } else if (impl == Library.java) {
      if (cmd == Command.GET)
        readURL(urlString);
      else if (cmd == Command.PUT)
        putURL(urlString);
      else
        openURL(urlString, cmd);
    }
  }

  /*
  ///////////////////////////////////////////////////////
  // Uses apache HttpComponents

  private void openClient(String urlString, Command cmd) {
    HttpEntity entity = null;
    try {
      org.apache.http.client.HttpClient httpclient = new DefaultHttpClient();

      // request
      HttpGet httpget = new HttpGet(urlString);
      appendLine("Request: " + httpget.getRequestLine());

      HttpParams params = httpget.getParams();
      appendLine("Params: ");
      showParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, params);
      showParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET, params);
      showParameter(CoreProtocolPNames.ORIGIN_SERVER, params);
      showParameter(CoreProtocolPNames.PROTOCOL_VERSION, params);
      showParameter(CoreProtocolPNames.STRICT_TRANSFER_ENCODING, params);
      showParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, params);
      showParameter(CoreProtocolPNames.USER_AGENT, params);
      showParameter(CoreProtocolPNames.WAIT_FOR_CONTINUE, params);

      //response
      BasicHttpContext localContext = new BasicHttpContext();
      HttpResponse response = httpclient.execute(httpget, localContext);

      appendLine("\nHttpContext: " + localContext);
      showAtribute(ExecutionContext.HTTP_CONNECTION, localContext);
      showAtribute(ExecutionContext.HTTP_PROXY_HOST, localContext);
      showAtribute(ExecutionContext.HTTP_REQ_SENT, localContext);
      showAtribute(ExecutionContext.HTTP_REQUEST, localContext);
      showAtribute(ExecutionContext.HTTP_RESPONSE, localContext);
      showAtribute(ExecutionContext.HTTP_TARGET_HOST, localContext);

      HttpRequest req = (HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
      appendLine("\nRequest Headers:");
      HeaderIterator it = req.headerIterator();
      while (it.hasNext()) {
        appendLine(" " + it.next().toString());
      }

      appendLine("\nResponse Headers:");
      it = response.headerIterator();
      while (it.hasNext()) {
        appendLine(" " + it.next().toString());
      }

      // content
      entity = response.getEntity();
      if (entity != null) {
        String contents = EntityUtils.toString(entity);
        if (contents.length() > 50 * 1000)
          contents = contents.substring(0, 50 * 1000);
        appendLine("\nContent:");
        appendLine(contents);
      }

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
      e.printStackTrace(new PrintStream(bos));
      appendLine(bos.toString());

    } finally {
      if (entity != null) try {
        entity.consumeContent();

      } catch (IOException e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        e.printStackTrace(new PrintStream(bos));
        appendLine(bos.toString());
      }
    }
  }

  private void showAtribute(String key, HttpContext localContext) {
    Object value = localContext.getAttribute(key);
    if (null != value)
      appendLine(" " + key + ": " + value);
  }

  private void showParameter(String key, HttpParams params) {
    Object value = params.getParameter(key);
    if (null != value)
      appendLine(" " + key + ": " + value);
  }  */

  ///////////////////////////////////////////////////////
  // Uses apache commons HttpClient

  private void openURL2(String urlString, Command cmd) {

    HttpClient httpclient = HttpClientManager.getHttpClient();
    HttpMethodBase m = null;

    try {
      /* you might think this works, but it doesnt:
      URI raw = new URI(urlString.trim());
      appendLine("raw scheme= " + raw.getScheme() + "\n auth= " + raw.getRawAuthority() + "\n path= " + raw.getRawPath() +
           "\n query= " + raw.getRawQuery() + "\n fragment= " + raw.getRawFragment()+"\n");

      URI uri = new URI(raw.getScheme(), raw.getRawAuthority(),
              URIUtil.encodePath(raw.getRawPath()),
              URIUtil.encodeQuery(raw.getRawQuery()),
              raw.getRawFragment());
      appendLine("encoded scheme= " + uri.getScheme() + "\n auth= " + uri.getAuthority() + "\n path= " + uri.getPath() +
           "\n query= " + uri.getQuery() + "\n fragment= " + uri.getFragment()+"\n");
      urlString = uri.toString();
              */

      urlString = URLnaming.escapeQuery(urlString);

      if (cmd == Command.GET)
        m = new GetMethod(urlString);
      else if (cmd == Command.HEAD)
        m = new HeadMethod(urlString);
      else if (cmd == Command.OPTIONS)
        m = new OptionsMethod(urlString);
      else if (cmd == Command.PUT) {
        PutMethod p = new PutMethod(urlString);
        try {
          p.setRequestEntity(new StringRequestEntity(ta.getText(), "application/text", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
          e.printStackTrace(new PrintStream(bos));
          appendLine(bos.toString());
          return;
        }
        m = p;
      }

      m.setRequestHeader("Accept-Encoding", "gzip,deflate");

      appendLine("HttpClient " + m.getName() + " " + urlString);
      appendLine("   do Authentication= " + m.getDoAuthentication());
      appendLine("   follow Redirects= " + m.getFollowRedirects());

      HttpMethodParams p = m.getParams();
      appendLine("   cookie policy= " + p.getCookiePolicy());
      appendLine("   http version= " + p.getVersion().toString());
      appendLine("   timeout (msecs)= " + p.getSoTimeout());
      appendLine("   virtual host= " + p.getVirtualHost());

      printHeaders("Request Headers = ", m.getRequestHeaders());
      appendLine(" ");

      httpclient.executeMethod(m);

      printHeaders("Request Headers2 = ", m.getRequestHeaders());
      appendLine(" ");

      appendLine("Status = " + m.getStatusCode() + " " + m.getStatusText());
      appendLine("Status Line = " + m.getStatusLine());
      printHeaders("Response Headers = ", m.getResponseHeaders());
      if (cmd == Command.GET) {
        appendLine("\nResponseBody---------------");

        String charset = m.getResponseCharSet();
        if (charset == null) charset = "UTF-8";
        String contents = null;

        // check for deflate and gzip compression
        Header h = m.getResponseHeader("content-encoding");
        String encoding = (h == null) ? null : h.getValue();

        if (encoding != null && encoding.equals("deflate")) {
          byte[] body = m.getResponseBody();
          InputStream is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(body)), 10000);
          contents = IO.readContents(is, charset);
          double ratio = (double) contents.length() / body.length;
          appendLine("  deflate encoded=" + body.length + " decoded=" + contents.length() + " ratio= " + ratio);

        } else if (encoding != null && encoding.equals("gzip")) {
          byte[] body = m.getResponseBody();
          InputStream is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(body)), 10000);
          contents = IO.readContents(is, charset);
          double ratio = (double) contents.length() / body.length;
          appendLine("  gzip encoded=" + body.length + " decoded=" + contents.length() + " ratio= " + ratio);

        } else {
          byte[] body = m.getResponseBody(50 * 1000); // max 50 Kbytes
          contents = new String(body, charset);
        }

        if (contents.length() > 50 * 1000)
          contents = contents.substring(0, 50 * 1000);
        appendLine(contents);

      } else if (cmd == Command.OPTIONS)
        printEnum("AllowedMethods = ", ((OptionsMethod) m).getAllowedMethods());

      printHeaders("Response Footers = ", m.getResponseFooters());

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
      e.printStackTrace(new PrintStream(bos));
      appendLine(bos.toString());

    } finally {
      if (m != null) m.releaseConnection();
    }
  }

  private void printHeaders(String title, Header[] heads) {
    appendLine(title);
    for (int i = 0; i < heads.length; i++) {
      Header head = heads[i];
      append("  " + head.toString());
    }
  }

  private void printEnum(String title, Enumeration en) {
    appendLine(title);
    while (en.hasMoreElements()) {
      Object o = en.nextElement();
      append("  " + o.toString());
    }
    appendLine("");
  }

  ///////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////
  // Uses java.net

  private void openURL(String urlString, Command command) {
    try {
      //Open the URLConnection for reading
      URL u = new URL(urlString);
      currentConnection = (HttpURLConnection) u.openConnection();
      currentConnection.setRequestMethod(command.toString()); // GET or HEAD
      currentConnection.setAllowUserInteraction(true);

      clear();
      appendLine(command + " request for " + urlString);

      // request headers
      Map<String, List<String>> reqs = currentConnection.getRequestProperties();
      for (String key : reqs.keySet()) {
        append(" " + key + ": ");
        for (String v : reqs.get(key))
          append(v + " ");
        appendLine("");
      }
      appendLine("");
      appendLine("getFollowRedirects=" + HttpURLConnection.getFollowRedirects());
      appendLine("getInstanceFollowRedirects=" + currentConnection.getInstanceFollowRedirects());
      appendLine("AllowUserInteraction=" + currentConnection.getAllowUserInteraction());
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
        appendLine(" " + key + ": " + header);
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
      appendLine("GET request for " + urlString);

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
        appendLine(" " + key + ": " + header);
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

  /* public void setURL(String urlString) {
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
   }  */

  public void setURL(String urlString) {
    if (urlString == null) return;
    cb.addItem(urlString);
    clear();
  }

  void putURL(String urlString) {
    if (urlString == null) return;
    String contents = ta.getText();
    IO.HttpResult result = IO.putToURL(urlString, contents);
    javax.swing.JOptionPane.showMessageDialog(this, "Status code= " + result.statusCode + "\n" + result.message);
  }

  public void save() {
    cb.save();
  }

  public void append(String text) {
    ta.append(text);
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

    GetContentsTask(String urlString) {
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
        } catch (IOException ioe) {
        }
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