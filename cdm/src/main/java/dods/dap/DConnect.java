/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

import java.net.*;
import java.io.*;
import dods.dap.parser.ParseException;
import java.util.zip.InflaterInputStream;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.auth.CredentialsProvider;

/**
 * Rewritten 1/15/07 jcaron to use HttpCLient library instead of jdk UrlConnection class.
 * Need more robust redirect and authentication.
 *
 * This class provides support for common DODS client-side operations such as
 * dereferencing a DODS URL, communicating network activity status
 * to the user and reading local DODS objects.
 * <p>
 * Unlike its C++ counterpart, this class does not store instances of the DAS,
 * DDS, etc. objects. Rather, the methods <code>getDAS</code>, etc. return
 * instances of those objects.
 *
 * @version $Revision: 48 $
 * @author jehamby
 */
public class DConnect {
  static private boolean allowSessions = false;
  static private CredentialsProvider credentialsProvider = null;

  static public void setAllowSessions( boolean b) { allowSessions = b; }
  static public void setCredentialsProvider(CredentialsProvider provider) {
    credentialsProvider = provider;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  private HttpClient httpclient;

  private String urlString; // The current DODS URL without Constraint Expression
  private String projString;  /** The projection portion of the current DODS CE (including leading "?"). */
  private String selString; /** The selection portion of the current DODS CE (including leading "&"). */

  private boolean acceptDeflate; /** Whether to accept compressed documents. */

  // various stuff that comes from the HTTP headers
  private String lastModified = null;
  private String lastExtended = null;
  private String lastModifiedInvalid = null;
  private boolean hasSession = false;

  /** The DODS server version. */
  private ServerVersion ver;

  private boolean debugHeaders = false;


  /**
   * Creates an instance bound to url which accepts compressed documents.
   * @param urlString connect to this URL.
   * @exception FileNotFoundException thrown if <code>urlString</code> is not
   *     a valid URL, or a filename which exists on the system.
   * @see DConnect#DConnect(String, boolean)
   */
  public DConnect(String urlString) throws FileNotFoundException {
    this(urlString, true);
  }

  /**
   * Creates an instance bound to url. If <code>acceptDeflate</code> is true
   * then HTTP Request headers will indicate to servers that this client can
   * accept compressed documents.
   *
   * @param urlString Connect to this URL.  If urlString is not a valid URL,
   *   it is assumed to be a filename, which is opened.
   * @param acceptDeflate true if this client can accept responses encoded
   *   with deflate.
   * @exception FileNotFoundException thrown if <code>urlString</code> is not
   *   a valid URL, or a filename which exists on the system.
   */
  public DConnect(String urlString, boolean acceptDeflate) throws FileNotFoundException {
    int ceIndex = urlString.indexOf('?');
    if(ceIndex != -1) {
      this.urlString = urlString.substring(0, ceIndex);
      String expr = urlString.substring(ceIndex);
      int selIndex = expr.indexOf('&');
      if(selIndex != -1) {
        this.projString = expr.substring(0, selIndex);
        this.selString = expr.substring(selIndex);
      } else {
        this.projString = expr;
        this.selString = "";
      }
    } else {
      this.urlString = urlString;
      this.projString = this.selString = "";
    }
    this.acceptDeflate = acceptDeflate;

    httpclient = new HttpClient();
    if (credentialsProvider != null)
      httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, credentialsProvider);
    httpclient.getParams().setBooleanParameter( HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
  }

  /**
   * Returns the constraint expression supplied with the URL given to the
   * constructor. If no CE was given this returns an empty <code>String</code>.
   * <p>
   * Note that the CE supplied to one of this object's constructors is
   * "sticky"; it will be used with every data request made with this object.
   * The CE passed to <code>getData</code>, however, is not sticky; it is used
   * only for that specific request. This method returns the sticky CE.
   *
   * @return the constraint expression associated with this connection.
   */
  public final String CE() {
    return projString + selString;
  }

  /**
   * Returns the URL supplied to the constructor. If the URL contained a
   * constraint expression that is not returned.
   *
   * @return the URL of this connection.
   */
  public final String URL() {
    return urlString;
  }

  /**
   * Open a connection to the DODS server.
   * @param urlString the URL to open.
   * @param command execute this command on the input stream
   * @exception IOException if an IO exception occurred.
   * @exception DODSException if the DODS server returned an error.
   * @throws dods.dap.parser.ParseException is cant parse the return
   */
  private void openConnection(String urlString, Command command) throws IOException, DODSException, ParseException {

    GetMethod method = new GetMethod(urlString);
    method.setFollowRedirects( true);

    if (acceptDeflate)
      method.setRequestHeader(new Header("Accept-Encoding", "deflate"));

    // enable sessions
    if (allowSessions)
      method.setRequestHeader(new Header("X-Accept-Session", "true"));

    InputStream is = null;
    try {
      // Execute the method.
      int statusCode = httpclient.executeMethod(method);

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        throw new DODSException(DODSException.NO_SUCH_FILE , method.getStatusLine().toString());
      }

      if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
        throw new DODSException(DODSException.NO_AUTHORIZATION , method.getStatusLine().toString());
      }

      if (statusCode != HttpStatus.SC_OK) {
        throw new DODSException("Method failed:" + method.getStatusLine());
      }

      // Get the response body.
      is = method.getResponseBodyAsStream();

       // check is its an error
      Header header = method.getResponseHeader("content-description");
      if (header != null && header.getValue().equals("dods_error")) {
        // create server exception object
        DODSException ds = new DODSException();
        // parse the Error object from stream and throw it
        ds.parse(is);
        throw ds;
      }

      // parse the version
      Header h = method.getResponseHeader("xdods-server");
      String versionString = (h == null) ? null : h.getValue();
      ver = new ServerVersion(versionString);

      checkHeaders(method);

      // check for deflator
      h = method.getResponseHeader("content-encoding");
      String encoding = (h == null) ? null : h.getValue();
      if (encoding != null && encoding.equals("deflate")) {
        InflaterInputStream inflater = new InflaterInputStream(is);
        is = new BufferedInputStream(inflater, 10 * 1000);   // JC added
      }

      command.process(is);

    } catch (HttpException e) {
      throw new DODSException("Fatal protocol violation: " + e.getMessage());

    } catch (IOException e) {
      throw new IOException("Fatal transport error: " + e.getMessage());

    } finally {
      // Release the connection.
      if (is != null) is.close();
      method.releaseConnection();
    }
  }

  public void closeSession() {
    try {
      if (allowSessions && hasSession) {
        openConnection(urlString + ".close", new Command() {
          public void process(InputStream is) throws IOException {
             byte[] buffer = new byte[4096];  // read the body fully
              while (is.read(buffer) > 0) {
                 // empty
              }
          }
        });
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  private void checkHeaders( GetMethod method) {
    if (debugHeaders) System.out.println("\nOpenConnection Headers for "+method.getPath());
    if (debugHeaders) System.out.println("Status Line: " + method.getStatusLine());

    Header[] responseHeaders = method.getResponseHeaders();
    for (int i1 = 0; i1 < responseHeaders.length; i1++) {
      Header responseHeader = responseHeaders[i1];
      if (debugHeaders) System.out.print("  " + responseHeader);
      String key = responseHeader.getName();
      String value = responseHeader.getValue();

      if (key.equals("Last-Modified")) {
        lastModified = value;
        if (debugHeaders) System.out.println(" **found lastModified = " + lastModified);

      } else if (key.equals("X-Last-Extended")) {
        lastExtended = value;
        if (debugHeaders) System.out.println(" **found lastExtended = " + lastExtended);

      } else if (key.equals("X-Last-Modified-Invalid")) {
        lastModifiedInvalid = value;
        if (debugHeaders) System.out.println(" **found lastModifiedInvalid = " + lastModifiedInvalid);
      }
    }

    if (debugHeaders) System.out.println("OpenConnection Headers for "+method.getPath());
    HttpState state = httpclient.getState();
    Cookie[] cookies = state.getCookies();

    if (cookies.length > 0) {
      if (debugHeaders) System.out.println("Cookies= ");

      for (int i = 0; i < cookies.length; i++) {
        Cookie cooky = cookies[i];
        if (debugHeaders) System.out.println("  "+cooky);
        if (cooky.getName().equalsIgnoreCase("jsessionid"))
          hasSession = true;
      }
    }

  }

  private interface Command {
    void process(InputStream is) throws DODSException, ParseException, IOException;
  }

  /**
   * Returns the DAS object from the dataset referenced by this object's URL.
   * The DAS object is referred to by appending `.das' to the end of a DODS
   * URL.
   *
   * @return the DAS associated with the referenced dataset.
   * @exception MalformedURLException if the URL given to the
   *   constructor has an error
   * @exception IOException if an error connecting to the remote server
   * @exception ParseException if the DAS parser returned an error
   * @exception DASException on an error constructing the DAS
   * @exception DODSException if an error returned by the remote server
   */
  public DAS getDAS() throws IOException, ParseException, DODSException {
    DASCommand command = new DASCommand();
    openConnection(urlString + ".das" + projString + selString, command);
    return command.das;
  }

  private class DASCommand implements Command {
    DAS das = new DAS();
    public void process(InputStream is) throws DASException, ParseException {
      das.parse(is);
    }
  }

  /**
   * Returns the DDS object from the dataset referenced by this object's URL.
   * The DDS object is referred to by appending `.dds' to the end of a DODS
   * URL.
   *
   * @return the DDS associated with the referenced dataset.
   * @exception MalformedURLException if the URL given to the constructor
   *    has an error
   * @exception IOException if an error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if an error returned by the remote server
   */
  public DDS getDDS() throws IOException, ParseException, DODSException {
    DDSCommand command = new DDSCommand();
    openConnection(urlString + ".dds" + projString + selString, command);
    return command.dds;
  }

  private class DDSCommand implements Command {
    DDS dds = new DDS();
    public void process(InputStream is) throws DODSException, ParseException {
      dds.parse(is);
    }
  }

  /**
   * Returns the `Data object' from the dataset referenced by this object's
   * URL given the constraint expression CE. Note that the Data object is
   * really just a DDS object with data bound to the variables. The DDS will
   * probably contain fewer variables (and those might have different
   * types) than in the DDS returned by getDDS() because that method returns
   * the entire DDS (but without any data) while this method returns
   * only those variables listed in the projection part of the constraint
   * expression.
   * <p>
   * Note that if CE is an empty String then the entire dataset will be
   * returned, unless a "sticky" CE has been specified in the constructor.
   *
   * @param CE The constraint expression to be applied to this request by the
   *    server.  This is combined with any CE given in the constructor.
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @return The <code>DataDDS</code> object that results from applying the
   *    given CE, combined with this object's sticky CE, on the referenced
   *    dataset.
   * @exception MalformedURLException if the URL given to the constructor
        has an error
   * @exception IOException if any error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if any error returned by the remote server
   * @param btf sumthin' good
   */
  public DataDDS getData(String CE, StatusUI statusUI, BaseTypeFactory btf) throws IOException,
          ParseException, DODSException {

    String localProjString, localSelString;
    int selIndex = CE.indexOf('&');
    if (selIndex != -1) {
      localProjString = CE.substring(0, selIndex);
      localSelString = CE.substring(selIndex);
    } else {
      localProjString = CE;
      localSelString = "";
    }
    String urls = urlString + ".dods" + projString + localProjString + selString + localSelString;

    DataDDS dds = new DataDDS(ver, btf);
    DataDDSCommand command = new DataDDSCommand( dds, statusUI);
    openConnection(urls, command);

    return command.dds;
  }

  private class DataDDSCommand implements Command {
    DataDDS dds;
    StatusUI statusUI;
    DataDDSCommand( DataDDS dds, StatusUI statusUI) {
      this.dds = dds;
      this.statusUI = statusUI;
    }
    public void process(InputStream is) throws DODSException, ParseException, IOException {
      dds.parse(new HeaderInputStream(is));
      dds.readData(is, statusUI);
    }
  }

  /**
   * Returns the `Data object' from the dataset referenced by this object's
   * URL given the constraint expression CE. Note that the Data object is
   * really just a DDS object with data bound to the variables. The DDS will
   * probably contain fewer variables (and those might have different
   * types) than in the DDS returned by getDDS() because that method returns
   * the entire DDS (but without any data) while this method returns
   * only those variables listed in the projection part of the constraint
   * expression.
   * <p>
   * Note that if CE is an empty String then the entire dataset will be
   * returned, unless a "sticky" CE has been specified in the constructor.
   *
   * @param CE The constraint expression to be applied to this request by the
   *    server.  This is combined with any CE given in the constructor.
   * @return The <code>DataDDS</code> object that results from applying the
   *    given CE, combined with this object's sticky CE, on the referenced
   *    dataset.
   * @exception MalformedURLException if the URL given to the constructor
        has an error
   * @exception IOException if any error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if any error returned by the remote server
   */
  public DataDDS getData(String CE) throws IOException, ParseException, DODSException {

    return getData(CE, null, new DefaultFactory());
  }

  public DataDDS getData(String CE, StatusUI statusUI) throws IOException, ParseException, DODSException {

    return getData(CE, statusUI, new DefaultFactory());
  }

  /**
   * Returns the <code>ServerVersion</code> of the last connection.
   * @return the <code>ServerVersion</code> of the last connection.
   */
  public final ServerVersion getServerVersion() {
    return ver;
  }

  /** @return value of Last-Modified Header, may be null */
  public String getLastModifiedHeader() { return lastModified; }

  /** @return  value of X-Last-Modified-Invalid Header, may be null */
  public String getLastModifiedInvalidHeader() { return lastModifiedInvalid; }

  /** @return  value of Last-Extended Header, may be null */
  public String getLastExtendedHeader() { return lastExtended; }

}

/*

http://forum.java.sun.com/thread.jspa?threadID=546542&messageID=4154990

public String getWebPage(String baseServer, String url, String webApp) {
		//Set Cookie Policy to be generically compatible.
	    	String url = baseServer + url;
		HttpClient client = new HttpClient();
		client.getState().setCookiePolicy(CookiePolicy.COMPATIBILITY);


		/***********************************************************************
		 * Get Method: Request secure page and get redirected to login page
		 **********************************************************************
		GetMethod authget = new GetMethod(url);
		try {
			client.executeMethod(authget);
		} catch (HttpException httpe) {
			LOG.error(httpe.getMessage(), httpe);
		} catch (IOException ioe) {
			LOG.error(ioe.getMessage(), ioe);
		}
		String responseBody = authget.getResponseBodyAsString();

		NameValuePair[] data = new NameValuePair[2];
		data[0] = new NameValuePair("j_username", "test");
		data[1] = new NameValuePair("j_password", "pa55w0rd");


		/***********************************************************************
		 * Post Method: logs into url
		 **********************************************************************
		String testURL = (baseServer + webApp + "/j_security_check");
		PostMethod authpost = new PostMethod((baseServer + webApp + "/j_security_check"));
		authpost.setRequestBody(data);
		authpost.setRequestHeader(authget.getRequestHeader("Cookie"));
		authpost.setRequestHeader(authget.getRequestHeader("Host"));
		authpost.setRequestHeader(authget.getRequestHeader("User-Agent"));

		//Release Get Connection
		authget.releaseConnection();

		try {
			client.executeMethod(authpost);
		} catch (HttpException httpe) {
			System.err.print("HttpException");
			System.err.println(httpe.getMessage());
			httpe.printStackTrace();
		} catch (IOException ioe) {
			System.err.print("IOException");
			System.err.println(ioe.getMessage());
			ioe.printStackTrace();
		}
		authget.setRequestHeader(authpost.getRequestHeader("Cookie"));
		authget.setRequestHeader(authpost.getRequestHeader("Host"));
		authget.setRequestHeader(authpost.getRequestHeader("User-Agent"));

		authpost.releaseConnection();

		/***********************************************************************
		 * Get Method: get content of page desired
		 **********************************************************************
		authget = new GetMethod(url);
		try {
			client.executeMethod(authget);
		} catch (HttpException httpe) {
			LOG.error(httpe.getMessage(), httpe);
		} catch (IOException ioe) {
			LOG.error(ioe.getMessage(), ioe);
		}
		responseBody = authget.getResponseBodyAsString();

		//Parse and process page

		//Release Get Connection
		authget.releaseConnection();

		/***********************************************************************
		 * Get Method: logout to release connection
		 **********************************************************************
		authget = new GetMethod((baseServer + webApp + "/logout.jsp"));
		try {
			client.executeMethod(authget);
		} catch (HttpException httpe) {
			LOG.error(httpe.getMessage(), httpe);
		} catch (IOException ioe) {
			LOG.error(ioe.getMessage(), ioe);
		}
		return responseBody;
	}

*/