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

package thredds.server.opendap;

import opendap.servlet.*;
import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.BaseType;
import opendap.dap.NoSuchVariableException;
import opendap.dap.Server.*;
import opendap.dap.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.net.URI;

import thredds.servlet.*;
import thredds.servlet.filter.CookieFilter;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.NetcdfFile;

/**
 * THREDDS opendap server.
 *
 * @author jcaron
 * @author Nathan David Potter
 * 
 * @since Apr 27, 2009 (branched)
 */
public class OpendapServlet extends javax.servlet.http.HttpServlet {
  static final String GDATASET = "guarded_dataset";
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpendapServlet.class);

  private boolean allowDeflate = false; // handled by Tomcat

  private String odapVersionString = "opendap/3.7";

  private URI baseURI = null;

  private int ascLimit = 50;
  private int binLimit = 500;

  private boolean debugSession = false;

  public void init() throws javax.servlet.ServletException {
    super.init();

    org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
    logServerStartup.info(getClass().getName() + " initialization start - " + UsageLog.setupNonRequestContext());

    this.ascLimit = ThreddsConfig.getInt( "Opendap.ascLimit", ascLimit);
    this.binLimit = ThreddsConfig.getInt( "Opendap.binLimit", binLimit);

    this.odapVersionString = ThreddsConfig.get( "Opendap.serverVersion", odapVersionString);
    logServerStartup.info(getClass().getName() + " version= "+odapVersionString+" ascLimit = "+ascLimit+" binLimit = "+binLimit);

    // debugging actions
    makeDebugActions();

    logServerStartup.info(getClass().getName() + " initialization done - " + UsageLog.closingMessageNonRequestContext());
  }

  private String getServerVersion() {
    return this.odapVersionString;
  }

  // Servlets that support HTTP GET requests and can quickly determine their last modification time should
  // override this method. This makes browser and proxy caches work more effectively, reducing the load on
  // server and network resources.
  protected long getLastModified(HttpServletRequest req) {
    String query = req.getQueryString();
    if (query != null) return -1;

    String path = req.getPathInfo();
    if (path == null) return -1;

    if (path.endsWith(".asc"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".ascii"))
      path = path.substring(0, path.length() - 6);
    else if (path.endsWith(".das"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".dds"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".ddx"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".dods"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".html"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".info"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".opendap"))
      path = path.substring(0, path.length() - 5);
    else
      return -1;

    // if (null != DatasetHandler.findResourceControl( path)) return -1; // LOOK weird Firefox beahviour?

    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(path);
    if ((file != null) && file.exists())
      return file.lastModified();

    return -1;
  }

  /////////////////////////////////////////////////////////////////////////////


  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    log.info("doGet(): " + UsageLog.setupRequestContext(request));
    // System.out.printf("opendap doGet: req=%s%n%s%n", ServletUtil.getRequest(request), ServletUtil.showRequestDetail(this, request));

    String path = null;

    try {
      path = request.getPathInfo();
      log.debug("doGet path={}", path);
      if (thredds.servlet.Debug.isSet("showRequestDetail"))
        log.debug(ServletUtil.showRequestDetail(this, request));

      if (path == null) {
        log.info("doGet(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (baseURI == null) { // first time, set baseURI
        URI reqURI = ServletUtil.getRequestURI(request);
        // Build base URI from request (rather than hard-coding "/thredds/dodsC/").
        String baseUriString = request.getContextPath() + request.getServletPath() + "/";
        baseURI = reqURI.resolve( baseUriString);
        log.debug("doGet(): baseURI was set = {}", baseURI);
      }

      if (path.endsWith("latest.xml")) {
        DataRootHandler.getInstance().processReqForLatestDataset(this, request, response);
        return;
      }

      // Redirect all catalog requests at the root level.
      if (path.equals("/") || path.equals("/catalog.html") || path.equals("/catalog.xml")) {
        ServletUtil.sendPermanentRedirect(ServletUtil.getContextPath() + path, request, response);
        return;
      }

      // Make sure catalog requests match a dataRoot before trying to handle.
      if (path.endsWith("/") || path.endsWith("/catalog.html") || path.endsWith("/catalog.xml")) {
        if (!DataRootHandler.getInstance().hasDataRootMatch(path)) {
          log.info("doGet(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }
        
        if ( ! DataRootHandler.getInstance().processReqForCatalog( request, response ) )
          log.error( "doGet(): " + UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_FORWARD_FAILURE, -1 ) );

        return;
      }

      ReqState rs = new ReqState(request, response, getServletConfig(), getServerName());

      if (rs != null) {
        String dataSet = rs.getDataSet();
        String requestSuffix = rs.getRequestSuffix();

        if ((dataSet == null) || dataSet.equals("/") || dataSet.equals("")) {
          doGetDIR(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("blob")) {
          doGetBLOB(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("close")) {
          doClose(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("dds")) {
          doGetDDS(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("das")) {
          doGetDAS(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("ddx")) {
          doGetDDX(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("dods")) {
          doGetDAP2Data(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("asc") || requestSuffix.equalsIgnoreCase("ascii")) {
          doGetASC(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("info")) {
          doGetINFO(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("html") || requestSuffix.equalsIgnoreCase("htm")) {
          doGetHTML(request, response, rs);
        } else if (requestSuffix.equalsIgnoreCase("ver") || requestSuffix.equalsIgnoreCase("version") ||
                dataSet.equalsIgnoreCase("/version") || dataSet.equalsIgnoreCase("/version/")) {
          doGetVER(request, response, rs);
        } else if (dataSet.equalsIgnoreCase("/help") || dataSet.equalsIgnoreCase("/help/") ||
                dataSet.equalsIgnoreCase("/" + requestSuffix) || requestSuffix.equalsIgnoreCase("help")) {
          doGetHELP(request, response);
        } else {
          sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Unrecognized request");
          return;
        }

      } else {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Unrecognized request");
        return;
      }

      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

      // plain ol' 404
    } catch (FileNotFoundException e) {
      sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());

      // DAP2Exception bad url
    } catch (BadURLException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      dap2ExceptionHandler(e, response);

      // all other DAP2Exception
    } catch (DAP2Exception de) {
      int status = (de.getErrorCode() == DAP2Exception.NO_SUCH_FILE) ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
      if ((de.getErrorCode() != DAP2Exception.NO_SUCH_FILE) && (de.getErrorMessage() != null))
        log.info(de.getErrorMessage());
      log.info(UsageLog.closingMessageForRequestContext(status, -1));
      response.setStatus(status);
      dap2ExceptionHandler(de, response);

      // parsing, usually the CE
    } catch (ParseException pe) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      parseExceptionHandler(pe, response);

      // 403 - request too big
    } catch (UnsupportedOperationException e) {
      sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());

    } catch (java.net.SocketException e) {
      log.info("SocketException: " + e.getMessage(), e);
      log.info(UsageLog.closingMessageForRequestContext(ServletUtil.STATUS_CLIENT_ABORT, -1));

    } catch (IOException e) {
      String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
      if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
        log.info("ClientAbortException: " + e.getMessage());
        log.info(UsageLog.closingMessageForRequestContext(ServletUtil.STATUS_CLIENT_ABORT, -1));
        return;
      }

      log.error("path= " + path, e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

      // everything else
    } catch (Throwable t) {
      log.error("path= " + path, t);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
    }

  }

  private void doGetASC(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (ds == null) return;

      response.setHeader("XDODS-Server", getServerVersion());
      response.setContentType("text/plain");
      response.setHeader("Content-Description", "dods-ascii");

      log.debug("Sending OPeNDAP ASCII Data For: " + rs + "  CE: '" + rs.getConstraintExpression() + "'");

      ServerDDS dds = ds.getDDS();
      CEEvaluator ce = new CEEvaluator(dds);
      ce.parseConstraint(rs.getConstraintExpression());
      checkSize(dds, true);

      PrintWriter pw = new PrintWriter(response.getOutputStream());
      dds.printConstrained(pw);
      pw.println("---------------------------------------------");

      AsciiWriter writer = new AsciiWriter(); // could be static
      writer.toASCII(pw, dds, ds);

      // the way that getDAP2Data works
      // DataOutputStream sink = new DataOutputStream(bOut);
      // ce.send(myDDS.getName(), sink, ds);

      pw.flush();

    } finally { // release lock if needed
      if (ds != null) ds.release();
    }

  }

  private void doGetDAS(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (ds == null) return;

      response.setContentType("text/plain");
      response.setHeader("XDODS-Server", getServerVersion());
      response.setHeader("Content-Description", "dods-das");

      OutputStream Out = new BufferedOutputStream(response.getOutputStream());

      DAS myDAS = ds.getDAS();
      myDAS.print(Out);

    } finally { // release lock if needed
      if (ds != null) ds.release();
    }

  }

  private void doGetDDS(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      response.setContentType("text/plain");
      response.setHeader("XDODS-Server", getServerVersion());
      response.setHeader("Content-Description", "dods-dds");

      OutputStream out = new BufferedOutputStream(response.getOutputStream());

      ServerDDS myDDS = ds.getDDS();

      if (rs.getConstraintExpression().equals("")) { // No Constraint Expression?
        // Send the whole DDS
        myDDS.print(out);
        out.flush();

      } else { // Otherwise, send the constrained DDS
        // Instantiate the CEEvaluator and parse the constraint expression
        CEEvaluator ce = new CEEvaluator(myDDS);
        ce.parseConstraint(rs.getConstraintExpression());

        // Send the constrained DDS back to the client
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
        myDDS.printConstrained(pw);
        pw.flush();
      }

    } finally { // release lock if needed
      if (ds != null) ds.release();
    }

  }

  private void doGetDDX(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      response.setContentType("text/plain");
      response.setHeader("XDODS-Server", getServerVersion());
      response.setHeader("Content-Description", "dods-ddx");

      OutputStream out = new BufferedOutputStream(response.getOutputStream());

      ServerDDS myDDS = ds.getDDS();
      myDDS.ingestDAS(ds.getDAS());

      if (rs.getConstraintExpression().equals("")) { // No Constraint Expression?
        // Send the whole DDS
        myDDS.printXML(out);
        out.flush();
      } else { // Otherwise, send the constrained DDS

        // Instantiate the CEEvaluator and parse the constraint expression
        CEEvaluator ce = new CEEvaluator(myDDS);
        ce.parseConstraint(rs.getConstraintExpression());

        // Send the constrained DDS back to the client
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
        myDDS.printConstrainedXML(pw);
        pw.flush();
      }

    } finally { // release lock if needed
      if (ds != null) ds.release();
    }
  }

  private void doGetBLOB(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      response.setContentType("application/octet-stream");
      response.setHeader("XDODS-Server", getServerVersion());
      response.setHeader("Content-Description", "dods-blob");

      ServletOutputStream sOut = response.getOutputStream();
      OutputStream bOut;
      DeflaterOutputStream dOut = null;
      if (rs.getAcceptsCompressed() && allowDeflate) {
        response.setHeader("Content-Encoding", "deflate");
        dOut = new DeflaterOutputStream(sOut);
        bOut = new BufferedOutputStream(dOut);
      } else {
        bOut = new BufferedOutputStream(sOut);
      }

      ServerDDS myDDS = ds.getDDS();
      CEEvaluator ce = new CEEvaluator(myDDS);
      ce.parseConstraint(rs.getConstraintExpression());
      checkSize(myDDS, false);

      // Send the binary data back to the client
      DataOutputStream sink = new DataOutputStream(bOut);
      ce.send(myDDS.getName(), sink, ds);
      sink.flush();

      // Finish up sending the compressed stuff, but don't
      // close the stream (who knows what the Servlet may expect!)
      if (null != dOut)
        dOut.finish();
      bOut.flush();

    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }

  }

  private void doClose(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {
    String reqPath = rs.getDataSet();
    HttpSession session = request.getSession();
    session.removeAttribute(reqPath); // work done in the listener

    response.setHeader("XDODS-Server", getServerVersion()); // needed by client

    /* if (path.endsWith(".close")) {
      closeSession(request, response);
      response.setContentLength(0);
      log.info("doGet(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, 0));
      return;

    }

    // so we need to worry about deleting sessions?
    session.invalidate();  */
  }

  private void doGetDAP2Data(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      response.setContentType("application/octet-stream");
      response.setHeader("XDODS-Server", getServerVersion());
      response.setHeader("Content-Description", "dods-data");

      ServletOutputStream sOut = response.getOutputStream();
      OutputStream bOut;
      DeflaterOutputStream dOut = null;
      if (rs.getAcceptsCompressed() && allowDeflate) {
        response.setHeader("Content-Encoding", "deflate");
        dOut = new DeflaterOutputStream(sOut);
        bOut = new BufferedOutputStream(dOut);

      } else {
        bOut = new BufferedOutputStream(sOut);
      }

      ServerDDS myDDS = ds.getDDS();
      CEEvaluator ce = new CEEvaluator(myDDS);
      ce.parseConstraint(rs.getConstraintExpression());
      checkSize(myDDS, false);

      // Send the constrained DDS back to the client
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(bOut));
      myDDS.printConstrained(pw);

      // Send the Data delimiter back to the client
      pw.flush();
      bOut.write("\nData:\n".getBytes());
      bOut.flush();

      // Send the binary data back to the client
      DataOutputStream sink = new DataOutputStream(bOut);
      ce.send(myDDS.getName(), sink, ds);
      sink.flush();

      // Finish up sending the compressed stuff, but don't
      // close the stream (who knows what the Servlet may expect!)
      if (null != dOut)
        dOut.finish();
      bOut.flush();

    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }
  }

  private void doGetVER(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    response.setContentType("text/plain");
    response.setHeader("XDODS-Server", getServerVersion());
    response.setHeader("Content-Description", "dods-version");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

    pw.println("Server Version: " + getServerVersion());
    pw.flush();
  }

  private void doGetHELP(HttpServletRequest request, HttpServletResponse response) throws Exception {

    response.setContentType("text/html");
    response.setHeader("XDODS-Server", getServerVersion());
    response.setHeader("Content-Description", "dods-help");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
    printHelpPage(pw);
    pw.flush();
  }

  private void doGetDIR(HttpServletRequest req, HttpServletResponse res, ReqState rs) throws Exception {
    // rather dangerous here, since you can go into an infinite loop
    // so we're going to insist that there's  no suffix
    if ((rs.getRequestSuffix() == null) || (rs.getRequestSuffix().length() == 0)) {
      ServletUtil.forwardToCatalogServices(req, res);
      return;
    }

    sendErrorResponse(res, 0, "Unrecognized request");
  }

  private void doGetINFO(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      PrintStream pw = new PrintStream(response.getOutputStream());
      response.setHeader("XDODS-Server", getServerVersion());
      response.setContentType("text/html");
      response.setHeader("Content-Description", "dods-description");

      GetInfoHandler di = new GetInfoHandler();
      di.sendINFO(pw, ds, rs);

    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }
  }

  private void doGetHTML(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (ds == null) return;

      response.setHeader("XDODS-Server", getServerVersion());
      response.setContentType("text/html");
      response.setHeader("Content-Description", "dods-form");

      // Utilize the getDDS() method to get	a parsed and populated DDS
      // for this server.
      ServerDDS myDDS = ds.getDDS();
      DAS das = ds.getDAS();
      GetHTMLInterfaceHandler2 di = new GetHTMLInterfaceHandler2();
      di.sendDataRequestForm(request, response, rs.getDataSet(), myDDS, das);

    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }

  }


  ///////////////////////////////////////////////////////////////////////////////////////////////
  // debugging
  private void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("ncdodsServer");
    DebugHandler.Action act;


    act = new DebugHandler.Action("help", "Show help page") {
      public void doAction(DebugHandler.Event e) {
        try {
          doGetHELP(e.req, e.res);
        }
        catch (Exception ioe) {
          log.error("ShowHelp", ioe);
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("version", "Show server version") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("  version= " + getServerVersion());
      }
    };
    debugHandler.addAction(act);

  }

  private String getServerName() {
    return this.getClass().getName();
  }

  /**
   * ************************************************************************
   * Prints the OPeNDAP Server help page to the passed PrintWriter
   *
   * @param pw PrintWriter stream to which to dump the help page.
   */
  private void printHelpPage(PrintWriter pw) {

    pw.println("<h3>OPeNDAP Server Help</h3>");
    pw.println("To access most of the features of this OPeNDAP server, append");
    pw.println("one of the following a eight suffixes to a URL: .das, .dds, .dods, .ddx, .blob, .info,");
    pw.println(".ver or .help. Using these suffixes, you can ask this server for:");
    pw.println("<dl>");
    pw.println("<dt> das  </dt> <dd> Dataset Attribute Structure (DAS)</dd>");
    pw.println("<dt> dds  </dt> <dd> Dataset Descriptor Structure (DDS)</dd>");
    pw.println("<dt> dods </dt> <dd> DataDDS object (A constrained DDS populated with data)</dd>");
    pw.println("<dt> ddx  </dt> <dd> XML version of the DDS/DAS</dd>");
    pw.println("<dt> blob </dt> <dd> Serialized binary data content for requested data set, " +
            "with the constraint expression applied.</dd>");
    pw.println("<dt> info </dt> <dd> info object (attributes, types and other information)</dd>");
    pw.println("<dt> html </dt> <dd> html form for this dataset</dd>");
    pw.println("<dt> ver  </dt> <dd> return the version number of the server</dd>");
    pw.println("<dt> help </dt> <dd> help information (this text)</dd>");
    pw.println("</dl>");
    pw.println("For example, to request the DAS object from the FNOC1 dataset at URI/GSO (a");
    pw.println("test dataset) you would appand `.das' to the URL:");
    pw.println("http://opendap.gso.uri.edu/cgi-bin/nph-nc/data/fnoc1.nc.das.");

    pw.println("<p><b>Note</b>: Many OPeNDAP clients supply these extensions for you so you don't");
    pw.println("need to append them (for example when using interfaces supplied by us or");
    pw.println("software re-linked with a OPeNDAP client-library). Generally, you only need to");
    pw.println("add these if you are typing a URL directly into a WWW browser.");
    pw.println("<p><b>Note</b>: If you would like version information for this server but");
    pw.println("don't know a specific data file or data set name, use `/version' for the");
    pw.println("filename. For example: http://opendap.gso.uri.edu/cgi-bin/nph-nc/version will");
    pw.println("return the version number for the netCDF server used in the first example. ");

    pw.println("<p><b>Suggestion</b>: If you're typing this URL into a WWW browser and");
    pw.println("would like information about the dataset, use the `.info' extension.");

    pw.println("<p>If you'd like to see a data values, use the `.html' extension and submit a");
    pw.println("query using the customized form.");

  }
  //**************************************************************************


  /**
   * ************************************************************************
   * Prints the Bad URL Page page to the passed PrintWriter
   *
   * @param pw PrintWriter stream to which to dump the bad URL page.
   */
  private void printBadURLPage(PrintWriter pw) {

    String serverContactName = ThreddsConfig.get( "serverInformation.contact.name", "UNKNOWN" );
    String serverContactEmail = ThreddsConfig.get( "serverInformation.contact.email", "UNKNOWN" );
    pw.println("<h3>Error in URL</h3>");
    pw.println("The URL extension did not match any that are known by this");
    pw.println("server. Below is a list of the five extensions that are be recognized by");
    pw.println("all OPeNDAP servers. If you think that the server is broken (that the URL you");
    pw.println("submitted should have worked), then please contact the");
    pw.println("administrator of this server [" + serverContactName + "] at: ");
    pw.println("<a href='mailto:" + serverContactEmail + "'>" + serverContactEmail +"</a><p>");

  }

  ///////////////////////////////////////////////////////
  // utils

  private void checkSize(ServerDDS dds, boolean isAscii) {
    try {

      long size = 0;
      Enumeration vars = dds.getVariables();
      while (vars.hasMoreElements()) {
        BaseType bt = (BaseType) vars.nextElement();
        if (((ServerMethods) bt).isProject()) {

          if (bt instanceof SDArray) {
            SDArray da = (SDArray) bt;
            BaseType base = da.getPrimitiveVector().getTemplate();
            DataType dtype = DODSNetcdfFile.convertToNCType(base);
            int elemSize = dtype.getSize();
            int n = da.numDimensions();
            List<Range> ranges = new ArrayList<Range>(n);
            for (int i = 0; i < n; i++)
              ranges.add(new Range(da.getStart(i), da.getStop(i), da.getStride(i)));

            Section s = new Section(ranges);
            size += s.computeSize() * elemSize;

          } else if (bt instanceof SDGrid) {
            SDGrid grid = (SDGrid) bt;
            SDArray da = (SDArray) grid.getVar(0);
            BaseType base = da.getPrimitiveVector().getTemplate();
            DataType dtype = DODSNetcdfFile.convertToNCType(base);
            int elemSize = dtype.getSize();
            int n = da.numDimensions();
            List<Range> ranges = new ArrayList<Range>(n);
            for (int i = 0; i < n; i++)
              ranges.add(new Range(da.getStart(i), da.getStop(i), da.getStride(i)));
            Section s = new Section(ranges);
            size += s.computeSize() * elemSize;

          } /* else if (!(bt instanceof SDString)) {
            System.out.printf("OpendapServlet didnt count %s type= %s in size limit%n", bt.getName(), bt.getClass().getName());
          }  */
        }
      }
      log.debug("total size={}", size);
      double dsize = size / (1000 * 1000);
      double maxSize = isAscii ? ascLimit : binLimit; // Mbytes
      if (dsize > maxSize) {
        log.info("Reject request size = {} Mbytes", dsize);
        throw new UnsupportedOperationException("Request too big=" + dsize + " Mbytes, max=" + maxSize);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (InvalidParameterException e) {
      e.printStackTrace();
    } catch (NoSuchVariableException e) {
      e.printStackTrace();
    }
  }

  /*
   * *********************** dataset caching ***********************************************
   */

  // any time the server needs access to the dataset, it gets a "GuardedDataset" which allows us to add caching
  // optionally, a session may be established, which allows us to reserve the dataset for that session.
  private GuardedDataset getDataset(ReqState preq) throws Exception {
    HttpServletRequest req = preq.getRequest();
    String reqPath = preq.getDataSet();

    // see if the client wants sessions
    boolean acceptSession = false;
    String s = req.getHeader("X-Accept-Session");
    if (s != null && s.equalsIgnoreCase("true"))
      acceptSession = true;

    HttpSession session = null;
    if (acceptSession) {
      // see if theres already a session established, create one if not
      session = req.getSession();
      if (!session.isNew()) {
        GuardedDatasetImpl gdataset = (GuardedDatasetImpl) session.getAttribute(reqPath);
        if (null != gdataset) {
          if (debugSession) System.out.printf(" found gdataset %s in session %s %n", reqPath, session.getId());
          if (log.isDebugEnabled()) log.debug(" found gdataset " + gdataset + " in session " + session.getId());
          return gdataset;
        }
      }
    }

    NetcdfFile ncd = DatasetHandler.getNetcdfFile(req, preq.getResponse(), reqPath);
    if (null == ncd) return null;

    GuardedDatasetImpl gdataset = new GuardedDatasetImpl(reqPath, ncd, acceptSession);

    if (acceptSession) {
      String cookiePath = req.getRequestURI();
      String suffix = "."+preq.getRequestSuffix();
      if (cookiePath.endsWith(suffix)) // snip off the suffix
        cookiePath = cookiePath.substring( 0, cookiePath.length() - suffix.length());
      session.setAttribute(reqPath, gdataset);
      session.setAttribute(CookieFilter.SESSION_PATH, cookiePath);
      //session.setAttribute("dataset", ncd.getLocation());  // for UsageValve
      // session.setMaxInactiveInterval(30); // 30 second timeout !!
      if (debugSession) System.out.printf(" added gdataset %s in session %s cookiePath %s %n", reqPath, session.getId(), cookiePath);
      if (log.isDebugEnabled()) log.debug(" added gdataset " + gdataset + " in session " + session.getId());
    } /* else {
      session = req.getSession();
      session.setAttribute("dataset", ncd.getLocation()); // for UsageValve
    } */

    return gdataset;
  }

  //////////////////////////////////////////////////////////////////////////////

  private void parseExceptionHandler(ParseException pe, HttpServletResponse response) throws IOException {
    BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
    response.setHeader("Content-Description", "dods-error");
    response.setContentType("text/plain");

    String msg = pe.getMessage().replace('\"', '\'');

    DAP2Exception de2 = new DAP2Exception(opendap.dap.DAP2Exception.CANNOT_READ_FILE, msg);
    de2.print(eOut);
  }

  private void dap2ExceptionHandler(DAP2Exception de, HttpServletResponse response) throws IOException {
    response.setHeader("Content-Description", "dods-error");
    response.setContentType("text/plain");
    de.print(response.getOutputStream());
  }

  private void sendErrorResponse(HttpServletResponse response, int errorCode, String errorMessage) throws IOException {
    log.info(UsageLog.closingMessageForRequestContext(errorCode, -1));
    response.setStatus(errorCode);
    response.setHeader("Content-Description", "dods-error");
    response.setContentType("text/plain");

    PrintWriter pw = new PrintWriter(response.getOutputStream());
    pw.println("Error {");
    pw.println("    code = " + errorCode + ";");
    pw.println("    message = \"" + errorMessage + "\";");

    pw.println("};");
    pw.flush();
  }

}
