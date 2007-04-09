package thredds.util.net;

import java.io.*;
import java.util.Enumeration;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.*;

/**
 * Manage an HttpSession. Uses org.apache.commons.httpclient.
 * @deprecated use ucar.nc2.dataset.HttpClientManager
 * @author caron
 */
public class HttpSession {

  /* static {
    Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));
  } */

  static private HttpSession singleton;

  static public HttpSession getSession() {
    return singleton;
  }

  static public void setCredentialsProvider(CredentialsProviderExt provider) {
    singleton = new HttpSession( provider);
  }

  ////////////////////////////////////////////////
  private HttpClient httpclient;
  private int resultCode  = 0;
  private String resultText;

  private HttpSession(CredentialsProviderExt provider) {
    httpclient = new HttpClient();
    httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, provider);
    provider.setHttpSession( this);
  }

  public void setDefaultCredentials( AuthScope authScope, Credentials cred) {
    httpclient.getState().setCredentials( authScope,  cred);
    httpclient.getParams().setAuthenticationPreemptive(true);
  }

  public String getContent(String urlString) throws IOException {
     GetMethod m = new GetMethod(urlString);
     m.setDoAuthentication( true );
     m.setFollowRedirects( true );
     appendLine("HttpClient "+m.getName()+" "+urlString);

     try {
       open(m);
       appendLine("-------------------------------------------");
       return m.getResponseBodyAsString();

     } catch (IOException e) {
       e.printStackTrace();
       appendLine("IOException = "+e.getMessage());
       appendLine("-------------------------------------------");
       throw e;

     } finally {
       m.releaseConnection();
     }
   }

  public void putContent(String urlString, String content) throws IOException {
     PutMethod m = new PutMethod(urlString);
     m.setDoAuthentication( true );
     appendLine("HttpClient "+m.getName()+" "+urlString);

     try {
       m.setRequestBody(content);
       int resultCode = open(m);
       appendLine("-------------------------------------------");

       // followRedirect wont work for PUT
       if (resultCode == 302) {
         String redirectLocation;
         Header locationHeader = m.getResponseHeader("location");
         if (locationHeader != null) {
           redirectLocation = locationHeader.getValue();
           appendLine("***Follow Redirection = "+redirectLocation);
           putContent(redirectLocation, content);
         }
       }

     } catch (IOException e) {
       e.printStackTrace();
       appendLine("IOException = "+e.getMessage());
       appendLine("-------------------------------------------");
       throw e;

     } finally {
       m.releaseConnection();
     }
  }

  HttpMethodBase currentMethod = null;
  public InputStream getInputStream(String urlString) throws IOException {
    currentMethod = new GetMethod(urlString);
    currentMethod.setDoAuthentication(true);
    currentMethod.setFollowRedirects( true );
    appendLine("HttpClient " + currentMethod.getName() + " " + urlString);

    open( currentMethod);
    return currentMethod.getResponseBodyAsStream();
  }

  public void close() {
    if (currentMethod != null)
      currentMethod.releaseConnection();
    currentMethod = null;
  }

  private int open(HttpMethodBase m) throws IOException {
    appendLine("   do Authentication= "+m.getDoAuthentication());
    appendLine("   follow Redirects= "+m.getFollowRedirects());

    HttpMethodParams p = m.getParams();
    appendLine("   cookie policy= "+p.getCookiePolicy());
    appendLine("   http version= "+p.getVersion().toString());
    appendLine("   timeout (msecs)= "+p.getSoTimeout());
    appendLine("   virtual host= "+p.getVirtualHost());

    httpclient.executeMethod(m);
    printHeaders("Request Headers = ", m.getRequestHeaders());
    appendLine(" ");
    appendLine("Status = "+m.getStatusCode()+" "+m.getStatusText());
    appendLine("Status Line = "+m.getStatusLine());
    printHeaders("Response Headers = ", m.getResponseHeaders());
    printHeaders("Response Footers = ", m.getResponseFooters());

    resultCode = m.getStatusCode();
    resultText = m.getStatusText();
    return resultCode;
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

  public String getInfo() { return sbuff.toString(); }
  public int getResultCode() { return resultCode; }
  public String getResultText() { return resultText; }

  private StringBuffer sbuff = new StringBuffer();
  private void append(String text) { sbuff.append(text); }
  private void appendLine(String text) {
    sbuff.append(text);
    sbuff.append("\n");
  }

  /* private class ConsoleAuthPrompter implements CredentialsProvider {

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

}
