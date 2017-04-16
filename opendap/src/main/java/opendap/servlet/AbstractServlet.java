/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.servlet;

import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import javax.servlet.*;
import javax.servlet.http.*;

import opendap.dap.*;
import opendap.servers.*;
import opendap.dap.parsers.ParseException;
import opendap.util.Debug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.EscapeStrings;

/**
 * AbstractServlet is the base servlet class for an OPeNDAP
 * servers.
 * Default handlers for all of the acceptable OPeNDAP client
 * requests are here.
 * <p/>
 * Each of the request handlers appears as an adjunct method to
 * the doGet() method of the base servlet class. In order to
 * reduce the bulk of this file, many of these methods have been
 * in wrapper classes in this package (opendap.servlet).
 * <p/>
 * This is an abstract class because it is left to the individual
 * server development efforts to write the getDDS() and
 * getServerVersion() methods. The getDDS() method is intended to
 * be where the server specific OPeNDAP server data types are
 * used via their associated class factory.
 * <p/>
 * This code relies on the <code>javax.servlet.ServletConfig</code>
 * interface (in particular the <code>getInitParameter()</code> method)
 * to record detailed configuration information used by
 * the servlet and it's children.
 * <p/>
 * The servlet should be started in the servlet engine with the following
 * initParameters for the tomcat servlet engine:
 * <pre>
 *    &lt;servlet&gt;
 *        &lt;servlet-name&gt;
 *            dts
 *        &lt;/servlet-name&gt;
 *        &lt;servlet-class&gt;
 *            opendap.servers.test.dts
 *        &lt;/servlet-class&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;INFOcache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/info&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;DDScache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/dds&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;DAScache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/das&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;DDXcache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/ddx&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *    &lt;/servlet&gt;
 * <p/>
 * </pre>
 * <p/>
 * Obviously the actual values of these parameters will depend on your particular
 * file system.
 * <h3>See the file <i>SERVLETS</i> in the top level directory of the
 * software distribution for more detailed information about servlet
 * configuration. </h3>
 * Also, the method <code>processDodsURL()</code> could be overloaded
 * if some kind of special processing of the incoming request is needed
 * to ascertain the OPeNDAP URL information.
 *
 * @author Nathan David Potter
 * @author jcaron 2/7/07 merge changes
 * @see opendap.servlet.GetAsciiHandler
 * @see opendap.servlet.GetDirHandler
 * @see opendap.servlet.GetHTMLInterfaceHandler
 * @see opendap.servlet.GetInfoHandler
 * @see opendap.servlet.ReqState
 * @see opendap.servlet.ParsedRequest
 * @see opendap.servlet.GuardedDataset
 */


public abstract class AbstractServlet extends javax.servlet.http.HttpServlet {

    //////////////////////////////////////////////////
    // Statics
    static final boolean debug = false;

    // Define an overall logger for everyone to use
    // Start with a default logger, but allow an application to change it later
    static public org.slf4j.Logger log
             = org.slf4j.LoggerFactory.getLogger(AbstractServlet.class);

    static public void setLog(Class cl)
    {
        log = org.slf4j.LoggerFactory.getLogger(cl);
    }

    static public void printDODSException(opendap.dap.DAP2Exception de)
    {
        de.printStackTrace();
    }

    static public void printThrowable(Throwable t)
    {
      AbstractServlet.log.error(t.getMessage());
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      pw.close();
      String trace = null;
      try {sw.close(); trace = sw.toString(); } catch (IOException ioe) {trace = "unknown";}
      log.error(trace);
    }

  /**
   * ************************************************************************
   * Compression
   */

  protected boolean allowDeflate = true;

  /**
   * ************************************************************************
   * Debugging
   */
  private boolean track = false;


  /**
   * ************************************************************************
   * Used for thread syncronization.
   *
   * @serial
   */
  private Object syncLock = new Object();


  /*
   * ************************************************************************
   * Count "hits" on the server...
   *
   * @serial
   */
  private int HitCounter = 0;

    /**
     * Keep a copy of the servlet config
     */
    private ServletConfig servletConfig = null;

    /**
     * Keep a copy of the servlet context
     */
    private ServletContext servletContext = null;

    /**
     * path to the root of the servlet in tomcat webapps directory
     */
    private String rootpath = null;

    //////////////////////////////////////////////////////////
    // Constructor(s)

    public AbstractServlet()
    {
    }

    //////////////////////////////////////////////////////////
    // Get/Set

    /**
     * Getter function for rootpath
     * @return rootpath
     */
    public String getRootPath() {return rootpath;}

  /**
   * This function must be implemented locally for each OPeNDAP server. It should
   * return a String containing the OPeNDAP Server Version...
   *
   * @return The Server Version String
   */
  public abstract String getServerVersion();

  /**
   * ************************************************************************
   * This method must be implemented locally for each OPeNDAP server. The
   * local implementation of this method is the key piece for connecting
   * any localized data types that are derived from the opendap.server types
   * back into the running servlet.
   * <p/>
   * This method should do the following:
   * <ul>
   * <li> Make a new ServerFactory (aka BaseTypeFactory) for the dataset requested.
   * <li> Instantiate a ServerDDS using the ServerFactory and populate it (this
   * could be accomplished by just opening a (cached?) DDS in a file and parsing it)
   * <li> Return this freshly minted ServerDDS object (to the servlet code where it is used.)
   * </ul>
   *
   * @param rs The ReqState object for this particular client request.
   * @return The ServerDDS object all parsed and ready to roll.
   * @throws DAP2Exception
   * @throws IOException
   * @throws ParseException
   * @see opendap.servers.ServerDDS
   */
  protected abstract opendap.servlet.GuardedDataset getDataset(opendap.servlet.ReqState rs)
          throws Exception;


  /**
   * ************************************************************************
   * Intitializes the servlet. Init (at this time) basically sets up
   * the object opendap.util.util.Debug from the debuggery flags in the
   * servlet InitParameters. The Debug object can be referenced (with
   * impunity) from anywhere in the VM.
   */

  public void init() throws ServletException
  {
    super.init();

    // debuggering
    String debugOn = getInitParameter("DebugOn");
    if (debugOn != null) {
      StringTokenizer toker = new StringTokenizer(debugOn);
      while (toker.hasMoreTokens())
        Debug.set(toker.nextToken(), true);
    }

    servletConfig = this.getServletConfig();
    servletContext = servletConfig.getServletContext();
    rootpath = servletContext.getRealPath("/");

  }

  /**
   * ************************************************************************
   * Turns a ParseException into a OPeNDAP DAP2 error and sends it to the client.
   *
   * @param pe       The <code>ParseException</code> that caused the problem.
   * @param response The <code>HttpServletResponse</code> for the client.
   */
  public void parseExceptionHandler(ParseException pe, HttpServletResponse response) {
    log.error("DODSServlet.parseExceptionHandler", pe);

    if (Debug.isSet("showException")) {
      log.debug(pe.toString());
      printThrowable(pe);
    }

    //LogStream.out.println(pe);
    //pe.printStackTrace(LogStream.out);

    try {
      BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
      response.setHeader("Content-Description", "dods-error");

      // This should probably be set to "plain" but this works, the
      // C++ slients don't barf as they would if I sent "plain" AND
      // the C++ don't expect compressed data if I do this...
      response.setHeader("Content-Encoding", "");
      // response.setContentType("text/plain");  LOOK do we needLogStream.out

      // Strip any double quotes out of the parser error message.
      // These get stuck in auto-magically by the javacc generated parser
      // code and they break our error parser (bummer!)
      String msg = pe.getMessage().replace('\"', '\'');

      DAP2Exception de2 = new DAP2Exception(opendap.dap.DAP2Exception.CANNOT_READ_FILE, msg);
      de2.print(eOut);
    } catch (IOException ioe) {
      log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
    }

  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Sends a OPeNDAP DAP2 error to the client.
   *
   * @param de       The OPeNDAP DAP2 exception that caused the problem.
   * @param response The <code>HttpServletResponse</code> for the client.
   */
  public void dap2ExceptionHandler(DAP2Exception de, HttpServletResponse response) {
    log.info("DODSServlet.dodsExceptionHandler (" + de.getErrorCode() + ") " + de.getErrorMessage());

    if (Debug.isSet("showException")) {
      de.print(System.err);
      de.printStackTrace(System.err);
      printDODSException(de);
    }

    try {
      BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());

      response.setHeader("Content-Description", "dods-error");

      // This should probably be set to "plain" but this works, the
      // C++ slients don't barf as they would if I sent "plain" AND
      // the C++ don't expect compressed data if I do this...
      response.setHeader("Content-Encoding", "");
      de.print(eOut);

    } catch (IOException ioe) {
      log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
    }


  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Sends an error to the client.
   * LOOK: The problem is that if the message is already committed when the IOException occurs, the headers dont get set.
   *
   * @param e        The exception that caused the problem.
   * @param rs The <code>ReqState</code> for the client.
   */
  public void IOExceptionHandler(IOException e,  ReqState rs) {
    HttpServletResponse response = rs.getResponse();
    try {
      BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
      response.setHeader("Content-Description", "dods-error");

      // This should probably be set to "plain" but this works, the
      // C++ slients don't barf as they would if I sent "plain" AND
      // the C++ don't expect compressed data if I do this...
      response.setHeader("Content-Encoding", "");

      // Strip any double quotes out of the parser error message.
      // These get stuck in auto-magically by the javacc generated parser
      // code and they break our error parser (bummer!)
      String msg = e.getMessage();
      if (msg != null)
        msg = msg.replace('\"', '\'');

      DAP2Exception de2 = new DAP2Exception(opendap.dap.DAP2Exception.CANNOT_READ_FILE, msg);
      de2.print(eOut);

      if (Debug.isSet("showException")) {
        // Error message
        log.error("DODServlet ERROR (IOExceptionHandler): " + e);
        log.error(rs.toString());
        if (track) {
          RequestDebug reqD = (RequestDebug) rs.getUserObject();
          log.error("  request number: " + reqD.reqno + " thread: " + reqD.threadDesc);
        }
        printThrowable(e);
      }

    } catch (IOException ioe) {
      log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
    }

  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Sends an error to the client.
   *
   * @param e        The exception that caused the problem.
   * @param rs The <code>ReqState</code> for the client.
   */
  public void anyExceptionHandler(Throwable e, ReqState rs)
  {
    log.error("DODServlet ERROR (anyExceptionHandler): ",e);
    printThrowable(e);
    try {
      if(rs == null) {
        throw new DAP2Exception("anyExceptionHandler: no request state provided");
      }
      HttpServletResponse response = rs.getResponse();
      log.error(rs.toString());
      if (track) {
        RequestDebug reqD = (RequestDebug) rs.getUserObject();
        log.error("  request number: " + reqD.reqno + " thread: " + reqD.threadDesc);
      }
      response.setHeader("Content-Description", "dods-error");

      // This should probably be set to "plain" but this works, the
      // C++ slients don't barf as they would if I sent "plain" AND
      // the C++ don't expect compressed data if I do this...
      response.setHeader("Content-Encoding", "");

      // Strip any double quotes out of the parser error message.
      // These get stuck in auto-magically by the javacc generated parser
      // code and they break our error parser (bummer!)
      String msg = e.getMessage();
      if (msg != null)
        msg = msg.replace('\"', '\'');
      DAP2Exception de2 = new DAP2Exception(opendap.dap.DAP2Exception.UNDEFINED_ERROR, msg);
      BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
      de2.print(eOut);
    } catch (Exception ioe) {
      log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
    }

  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Sends a OPeNDAP DAP2 error (type UNKNOWN ERROR) to the client and displays a
   * message on the server console.
   *
   * @param request   The client's <code> HttpServletRequest</code> request object.
   * @param response  The server's <code> HttpServletResponse</code> response object.
   * @param clientMsg Error message <code>String</code> to send to the client.
   * @param serverMsg Error message <code>String</code> to display on the server console.
   */
  public void sendDODSError(HttpServletRequest request,
          HttpServletResponse response,
          String clientMsg,
          String serverMsg)
          throws IOException, ServletException {

    response.setContentType("text/plain");
    response.setHeader("XDODS-Server", getServerVersion());
    response.setHeader("Content-Description", "dods-error");
    // Commented because of a bug in the OPeNDAP C++ stuff...
    //response.setHeader("Content-Encoding", "none");

    ServletOutputStream Out = response.getOutputStream();

    DAP2Exception de = new DAP2Exception(opendap.dap.DAP2Exception.UNKNOWN_ERROR, clientMsg);

    de.print(Out);

    response.setStatus(HttpServletResponse.SC_OK);

    log.error(serverMsg);
  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's DAS request. Operates on the assumption
   * that the DAS information is cached on a disk local to the server. If you
   * don't like that, then you better override it in your server :)
   * <p/>
   * <p>Once the DAS has been parsed it is sent to the requesting client.
   *
   * @param rs       The ReqState of this client request. Contains all kinds of
   *                 important stuff.
   * @see ReqState
   */
  public void doGetDAS(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse")) {
     log.debug("doGetDAS for dataset: " + rs.getDataSet());
    }

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (ds == null) return;

      rs.getResponse().setContentType("text/plain");
      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setHeader("Content-Description", "dods-das");
      // Commented because of a bug in the OPeNDAP C++ stuff...
      //rs.getResponse().setHeader("Content-Encoding", "plain");

      OutputStream Out = new BufferedOutputStream(rs.getResponse().getOutputStream());

      DAS myDAS = ds.getDAS();
      myDAS.print(Out);
      rs.getResponse().setStatus(HttpServletResponse.SC_OK);
      if (Debug.isSet("showResponse")) {
       //log.debug("DAS=\n");
        //myDAS.print(LogStream.out);
      }
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    } finally { // release lock if needed
      if (ds != null) ds.release();
    }


  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's DDS request. Requires the getDDS() method
   * implemented by each server localization effort.
   * <p/>
   * <p>Once the DDS has been parsed and constrained it is sent to the
   * requesting client.
   *
   * @param rs       The ReqState of this client request. Contains all kinds of
   *                 important stuff.
   * @see ReqState
   */
  public void doGetDDS(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse"))
     log.debug("doGetDDS for dataset: " + rs.getDataSet());

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      rs.getResponse().setContentType("text/plain");
      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setHeader("Content-Description", "dods-dds");
      // Commented because of a bug in the OPeNDAP C++ stuff...
      //rs.getResponse().setHeader("Content-Encoding", "plain");

      OutputStream Out = new BufferedOutputStream(rs.getResponse().getOutputStream());

      // Utilize the getDDS() method to get a parsed and populated DDS
      // for this server.
      ServerDDS myDDS = ds.getDDS();

      if (rs.getConstraintExpression().equals("")) { // No Constraint Expression?
        // Send the whole DDS
        myDDS.print(Out);
        Out.flush();
      } else { // Otherwise, send the constrained DDS

        // Instantiate the CEEvaluator and parse the constraint expression
        CEEvaluator ce = new CEEvaluator(myDDS);
        ce.parseConstraint(rs);

        // Send the constrained DDS back to the client
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(Out,Util.UTF8));
        myDDS.printConstrained(pw);
        pw.flush();
      }

      rs.getResponse().setStatus(HttpServletResponse.SC_OK);
      if (Debug.isSet("showResponse")) {
        if (rs.getConstraintExpression().equals("")) { // No Constraint Expression?
         log.debug("Unconstrained DDS=\n");
          //myDDS.print();
        } else {
         log.debug("Constrained DDS=\n");
          //myDDS.printConstrained();
        }
      }

    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (IOException pe) {
      IOExceptionHandler(pe, rs);
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    } finally { // release lock if needed
      if (ds != null) ds.release();
    }


  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's DDS request. Requires the getDDS() method
   * implemented by each server localization effort.
   * <p/>
   * <p>Once the DDS has been parsed and constrained it is sent to the
   * requesting client.
   *
   * @param rs       The ReqState of this client request. Contains all kinds of
   *                 important stuff.
   * @see ReqState
   */
  public void doGetDDX(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse"))
     log.debug("doGetDDX for dataset: " + rs.getDataSet());

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      rs.getResponse().setContentType("text/plain");
      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setHeader("Content-Description", "dods-ddx");
      // Commented because of a bug in the OPeNDAP C++ stuff...
      // rs.getResponse().setHeader("Content-Encoding", "plain");

      OutputStream Out = new BufferedOutputStream(rs.getResponse().getOutputStream());

      // Utilize the getDDS() method to get a parsed and populated DDS
      // for this server.
      ServerDDS myDDS = ds.getDDS();

      if (rs.getConstraintExpression().equals("")) { // No Constraint Expression?
        // Send the whole DDS
        myDDS.printXML(Out);
        Out.flush();
      } else { // Otherwise, send the constrained DDS

        // Instantiate the CEEvaluator and parse the constraint expression
        CEEvaluator ce = new CEEvaluator(myDDS);
        ce.parseConstraint(rs);

        // Send the constrained DDS back to the client
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(Out,Util.UTF8));
        myDDS.printConstrainedXML(pw);
        pw.flush();
      }

      rs.getResponse().setStatus(HttpServletResponse.SC_OK);
      if (Debug.isSet("showResponse")) {
        if (rs.getConstraintExpression().equals("")) { // No Constraint Expression?
         log.debug("Unconstrained DDX=\n");
          myDDS.printXML(System.out);
        } else {
         log.debug("Constrained DDX=\n");
          myDDS.printConstrainedXML(System.out);
        }
      }
    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (IOException pe) {
      IOExceptionHandler(pe, rs);
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    } finally { // release lock if needed
      if (ds != null) ds.release();
    }


  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's data request. Requires the getDDS()
   * method implemented by each server localization effort.
   * <p/>
   * <p>Once the DDS has been parsed, the data is read (using the class in the
   * localized server factory etc.), compared to the constraint expression,
   * and then sent to the client.
   *
   * @param rs       The ReqState of this client request. Contains all kinds of
   *                 important stuff.
   * @opendap.ddx.experimental
   * @see ReqState
   */
  public void doGetBLOB(ReqState rs)
          throws Exception {


    if (Debug.isSet("showResponse")) {
     log.debug("doGetBLOB For: " + rs.getDataSet());
    }
    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      rs.getResponse().setContentType("application/octet-stream");
      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setHeader("Content-Description", "dods-blob");

      ServletOutputStream sOut = rs.getResponse().getOutputStream();
      OutputStream bOut;
      DeflaterOutputStream dOut = null;
      if (rs.getAcceptsCompressed() && allowDeflate) {
        rs.getResponse().setHeader("Content-Encoding", "deflate");
        dOut = new DeflaterOutputStream(sOut);
        bOut = new BufferedOutputStream(dOut);
      } else {
        // Commented out because of a bug in the OPeNDAP C++ stuff...
        //rs.getResponse().setHeader("Content-Encoding", "plain");
        bOut = new BufferedOutputStream(sOut);
      }

      // Utilize the getDDS() method to get	a parsed and populated DDS
      // for this server.
      ServerDDS myDDS = ds.getDDS();

      // Instantiate the CEEvaluator and parse the constraint expression
      CEEvaluator ce = new CEEvaluator(myDDS);
      ce.parseConstraint(rs.getConstraintExpression(),rs.getRequestURL().toString());

      // Send the binary data back to the client
      DataOutputStream sink = new DataOutputStream(bOut);
      ce.send(myDDS.getEncodedName(), sink, ds);
      sink.flush();

      // Finish up sending the compressed stuff, but don't
      // close the stream (who knows what the Servlet may expect!)
      if (null != dOut)
        dOut.finish();
      bOut.flush();

      rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (IOException ioe) {
      IOExceptionHandler(ioe, rs);
    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }

  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's data request. Requires the getDDS()
   * method implemented by each server localization effort.
   * <p/>
   * <p>Once the DDS has been parsed, the data is read (using the class in the
   * localized server factory etc.), compared to the constraint expression,
   * and then sent to the client.
   *
   * @param rs       The ReqState of this client request. Contains all kinds of
   *                 important stuff.
   * @throws IOException
   * @throws ServletException
   * @see ReqState
   */
  public void doGetDAP2Data(ReqState rs)
          throws Exception {


    if (Debug.isSet("showResponse")) {
     log.debug("doGetDAP2Data For: " + rs.getDataSet());
    }
    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      rs.getResponse().setContentType("application/octet-stream");
      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setHeader("Content-Description", "dods-data");

      ServletOutputStream sOut = rs.getResponse().getOutputStream();
      OutputStream bOut;
      DeflaterOutputStream dOut = null;
      if (rs.getAcceptsCompressed() && allowDeflate) {
        rs.getResponse().setHeader("Content-Encoding", "deflate");
        dOut = new DeflaterOutputStream(sOut);
        bOut = new BufferedOutputStream(dOut);

      } else {
        // Commented out because of a bug in the OPeNDAP C++ stuff...
        //rs.getResponse().setHeader("Content-Encoding", "plain");
        bOut = new BufferedOutputStream(sOut);
      }

      // Utilize the getDDS() method to get	a parsed and populated DDS
      // for this server.
      ServerDDS myDDS = ds.getDDS();

      // Instantiate the CEEvaluator and parse the constraint expression
      CEEvaluator ce = new CEEvaluator(myDDS);
      ce.parseConstraint(rs);

      // debug
      //log.debug("CE DDS = ");
      // myDDS.printConstrained(LogStream.out);

      // Send the constrained DDS back to the client
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(bOut,Util.UTF8));
      myDDS.printConstrained(pw);

      // Send the Data delimiter back to the client
      //pw.println("Data:"); // JCARON CHANGED
      pw.flush();
      bOut.write("\nData:\n".getBytes(CDM.utf8Charset)); // JCARON CHANGED
      bOut.flush();

      // Send the binary data back to the client
      DataOutputStream sink = new DataOutputStream(bOut);
      ce.send(myDDS.getEncodedName(), sink, ds);
      sink.flush();

      // Finish up tsending the compressed stuff, but don't
      // close the stream (who knows what the Servlet may expect!)
      if (null != dOut)
        dOut.finish();
      bOut.flush();

      rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (IOException ioe) {
      IOExceptionHandler(ioe, rs);
    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }

  }

  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's directory request.
   * <p/>
   * Returns an html document to the client showing (a possibly pseudo)
   * listing of the datasets available on the server in a directory listing
   * format.
   * <p/>
   * The bulk of this code resides in the class opendap.servlet.GetDirHandler and
   * documentation may be found there.
   *
   * @param rs  The client's <code> ReqState</code>
   * @see opendap.servlet.GetDirHandler
   */
  public void doGetDIR(ReqState rs)
          throws Exception {


    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setContentType("text/html");
    rs.getResponse().setHeader("Content-Description", "dods-directory");

    try {
      GetDirHandler di = new GetDirHandler();
      di.sendDIR(rs);
      rs.getResponse().setStatus(HttpServletResponse.SC_OK);
    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    }

  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's version request.
   * <p/>
   * <p>Returns a plain text document with server version and OPeNDAP core
   * version #'s
   *
   * @param rs  The client's <code> ReqState</code>
   */
  public void doGetVER(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse")) {
     log.debug("Sending Version Tag.");
    }
    rs.getResponse().setContentType("text/plain");
    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setHeader("Content-Description", "dods-version");
    // Commented because of a bug in the OPeNDAP C++ stuff...
    //rs.getResponse().setHeader("Content-Encoding", "plain");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));

    pw.println("Server Version: " + getServerVersion());
    pw.flush();

    rs.getResponse().setStatus(HttpServletResponse.SC_OK);
  }

  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for the client's help request.
   * <p/>
   * <p> Returns an html page of help info for the server
   *
   * @param rs  The client's <code> ReqState </code>
   */
  public void doGetHELP(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse")) {
     log.debug("Sending Help Page.");
    }
    rs.getResponse().setContentType("text/html");
    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setHeader("Content-Description", "dods-help");
    // Commented because of a bug in the OPeNDAP C++ stuff...
    //rs.getResponse().setHeader("Content-Encoding", "plain");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
    printHelpPage(pw);
    pw.flush();

    rs.getResponse().setStatus(HttpServletResponse.SC_OK);
  }

  /***************************************************************************/


  /**
   * ************************************************************************
   * Sends an html document to the client explaining that they have used a
   * poorly formed URL and then the help page...
   *
   * @param rs  The client's <code> ReqState </code>
   */
  public void badURL(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse")) {
     log.debug("Sending Bad URL Page.");
    }

    //log.info("DODSServlet.badURL " + rs.getRequest().getRequestURI());

    rs.getResponse().setContentType("text/html");
    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setHeader("Content-Description", "dods-error");
    // Commented because of a bug in the OPeNDAP C++ stuff...
    //rs.getResponse().setHeader("Content-Encoding", "plain");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));

    printBadURLPage(pw);
    printHelpPage(pw);
    pw.flush();

    rs.getResponse().setStatus(HttpServletResponse.SC_OK);


  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for OPeNDAP ascii data requests. Returns the request data as
   * a comma delimited ascii file. Note that this means that the more complex
   * OPeNDAP structures such as Grids get flattened...
   * <p/>
   * <p/>
   * Modified 2/8/07 jcaron to not make a DConnect2 call to itself
   *
   * @param rs       the decoded Request State
   */
  public void doGetASC(ReqState rs) throws Exception {
    if (Debug.isSet("showResponse")) {
     log.debug("doGetASC For: " + rs.getDataSet());
    }

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (ds == null) return;

      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setContentType("text/plain");
      rs.getResponse().setHeader("Content-Description", "dods-ascii");

      if (debug)
       log.debug("Sending OPeNDAP ASCII Data For: " + rs + "  CE: '" + rs.getConstraintExpression() + "'");

      ServerDDS dds = ds.getDDS();
      //dds = url.getData(ce, null, new asciiFactory());  previous way

      // Instantiate the CEEvaluator and parse the constraint expression
      CEEvaluator ce = new CEEvaluator(dds);

      // i think this makes the dds constrained
      ce.parseConstraint(rs);

      PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
      dds.printConstrained(pw);
      pw.println("---------------------------------------------");




      AsciiWriter writer = new AsciiWriter(); // could be static
      writer.toASCII(pw,dds,ds);


      // the way that getDAP2Data works
      // DataOutputStream sink = new DataOutputStream(bOut);
      // ce.send(myDDS.getName(), sink, ds);

      pw.flush();

      rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    } finally { // release lock if needed
      if (ds != null) ds.release();
    }

  }

  /**
   * ************************************************************************
   * Default handler for OPeNDAP info requests. Returns an HTML document
   * describing the contents of the servers datasets.
   * <p/>
   * The bulk of this code resides in the class opendap.servlet.GetInfoHandler and
   * documentation may be found there.
   *
   * @param rs  The client's <code> ReqState </code>
   * @see GetInfoHandler
   */
  public void doGetINFO(ReqState rs)
          throws Exception {

    if (Debug.isSet("showResponse")) {
     log.debug("doGetINFO For: " + rs.getDataSet());
    }

    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (null == ds) return;

      PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setContentType("text/html");
      rs.getResponse().setHeader("Content-Description", "dods-description");

      GetInfoHandler di = new GetInfoHandler();
      di.sendINFO(pw, ds, rs);
      rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (IOException pe) {
      IOExceptionHandler(pe, rs);
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }

  }
  /**************************************************************************/


  /**
   * ************************************************************************
   * Default handler for OPeNDAP .html requests. Returns the OPeNDAP Web
   * Interface (aka The Interface From Hell) to the client.
   * <p/>
   * The bulk of this code resides in the class
   * opendap.servlet.GetHTMLInterfaceHandler and
   * documentation may be found there.
   *
   * @param rs  The client's <code> ReqState</code>
   * @see GetHTMLInterfaceHandler
   */
  public void doGetHTML(ReqState rs)
          throws Exception {


    GuardedDataset ds = null;
    try {
      ds = getDataset(rs);
      if (ds == null) return;

      rs.getResponse().setHeader("XDODS-Server", getServerVersion());
      rs.getResponse().setContentType("text/html");
      rs.getResponse().setHeader("Content-Description", "dods-form");

      // Utilize the getDDS() method to get	a parsed and populated DDS
      // for this server.
      ServerDDS myDDS = ds.getDDS();
      DAS das = ds.getDAS();
      GetHTMLInterfaceHandler di = new GetHTMLInterfaceHandler();
      di.sendDataRequestForm(rs, rs.getDataSet(), myDDS, das);
      rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    } catch (ParseException pe) {
      parseExceptionHandler(pe, rs.getResponse());
    } catch (DAP2Exception de) {
      dap2ExceptionHandler(de, rs.getResponse());
    } catch (IOException pe) {
      IOExceptionHandler(pe, rs);
    } catch (Throwable t) {
      anyExceptionHandler(t, rs);
    } finally {  // release lock if needed
      if (ds != null) ds.release();
    }

  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * Default handler for OPeNDAP catalog.xml requests.
   *
   * @param rs  The client's <code> ReqState </code>
   * @see GetHTMLInterfaceHandler
   */
  public void doGetCatalog(ReqState rs)
          throws Exception {


    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setContentType("text/xml");
    rs.getResponse().setHeader("Content-Description", "dods-catalog");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
    printCatalog(rs, pw);
    pw.flush();
    rs.getResponse().setStatus(HttpServletResponse.SC_OK);

  }

  // to be overridden by servers that implement catalogs
  protected void printCatalog(ReqState rs, PrintWriter os) throws IOException {
    os.println("Catalog not available for this server");
    os.println("Server version = " + getServerVersion());
  }

  /***************************************************************************/

  /**
   * ************************************************************************
   * Default handler for debug requests;
   *
   * @param rs  The client's <code> ReqState </code>  object.
   */
  public void doDebug(ReqState rs) throws IOException {


    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setContentType("text/html");
    rs.getResponse().setHeader("Content-Description", "dods_debug");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
    pw.println("<title>Debugging</title>");
    pw.println("<body><pre>");

    StringTokenizer tz = new StringTokenizer(rs.getConstraintExpression(), "=;");
    while (tz.hasMoreTokens()) {
      String cmd = tz.nextToken();
      pw.println("Cmd= " + cmd);

      if (cmd.equals("help")) {
        pw.println(" help;log;logEnd;logShow");
        pw.println(" showFlags;showInitParameters;showRequest");
        pw.println(" on|off=(flagName)");
        doDebugCmd(cmd, tz, pw); // for subclasses
      } else if (cmd.equals("on"))
        Debug.set(tz.nextToken(), true);

      else if (cmd.equals("off"))
        Debug.set(tz.nextToken(), false);

      else if (cmd.equals("showFlags")) {
        Iterator iter = Debug.keySet().iterator();
        while (iter.hasNext()) {
          String key = (String) iter.next();
          pw.println("  " + key + " " + Debug.isSet(key));
        }
      } else if (cmd.equals("showInitParameters"))
        pw.println(rs.toString());

      else if (cmd.equals("showRequest"))
        probeRequest(pw, rs);

      else if (!doDebugCmd(cmd, tz, pw)) { // for subclasses
        pw.println("  unrecognized command");
      }
    }

    pw.println("--------------------------------------");
    pw.println("Logging is on");
    Iterator iter = Debug.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      boolean val = Debug.isSet(key);
      if (val)
        pw.println("  " + key + " " + Debug.isSet(key));
    }

    pw.println("</pre></body>");
    pw.flush();
    rs.getResponse().setStatus(HttpServletResponse.SC_OK);

  }

  protected boolean doDebugCmd(String cmd, StringTokenizer tz, PrintWriter pw) {
    return false;
  }

  /**
   * ************************************************************************
   * Default handler for OPeNDAP status requests; not publically available,
   * used only for debugging
   *
   * @param rs  The client's <code> ReqState </code>
   * @see GetHTMLInterfaceHandler
   */
  public void doGetSystemProps(ReqState rs)
          throws Exception {

    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setContentType("text/html");
    rs.getResponse().setHeader("Content-Description", "dods-status");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(), Util.UTF8));
    pw.println("<html>");
    pw.println("<title>System Properties</title>");
    pw.println("<hr>");
    pw.println("<body><h2>System Properties</h2>");
    pw.println("<h3>Date: " + new Date() + "</h3>");

    Properties sysp = System.getProperties();
    Enumeration e = sysp.propertyNames();

    pw.println("<ul>");
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();

      String value = System.getProperty(name);

      pw.println("<li>" + name + ": " + value + "</li>");
    }
    pw.println("</ul>");

    pw.println("<h3>Runtime Info:</h3>");

    Runtime rt = Runtime.getRuntime();
    pw.println("JVM Max Memory:   " + (rt.maxMemory() / 1024) / 1000. + " MB (JVM Maximum Allowable Heap)<br>");
    pw.println("JVM Total Memory: " + (rt.totalMemory() / 1024) / 1000. + " MB (JVM Heap size)<br>");
    pw.println("JVM Free Memory:  " + (rt.freeMemory() / 1024) / 1000. + " MB (Unused part of heap)<br>");
    pw.println("JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024) / 1000. + " MB (Currently active memory)<br>");

    pw.println("<hr>");
    pw.println("</body>");
    pw.println("</html>");
    pw.flush();
    rs.getResponse().setStatus(HttpServletResponse.SC_OK);

  }


  /**
   * ************************************************************************
   * Default handler for OPeNDAP status requests; not publically available,
   * used only for debugging
   *
   * @param rs  The client's <code> ReqState</code>
   * @see GetHTMLInterfaceHandler
   */
  public void doGetStatus(ReqState rs)
          throws Exception {


    rs.getResponse().setHeader("XDODS-Server", getServerVersion());
    rs.getResponse().setContentType("text/html");
    rs.getResponse().setHeader("Content-Description", "dods-status");

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
    pw.println("<title>Server Status</title>");
    pw.println("<body><ul>");
    printStatus(pw);
    pw.println("</ul></body>");
    pw.flush();
    rs.getResponse().setStatus(HttpServletResponse.SC_OK);
  }


  // to be overridden by servers that implement status report
  protected void printStatus(PrintWriter os) throws IOException {
    os.println("<h2>Server version = " + getServerVersion() + "</h2>");
    os.println("<h2>Number of Requests Received = " + HitCounter + "</h2>");
    if (track) {
      int n = prArr.size();
      int pending = 0;
      StringBuilder preqs = new StringBuilder();
      for (int i = 0; i < n; i++) {
        ReqState rs = (ReqState) prArr.get(i);
        RequestDebug reqD = (RequestDebug) rs.getUserObject();
        if(!reqD.done) {
          preqs.append("<pre>-----------------------\n");
          preqs.append("Request[");
          preqs.append(reqD.reqno);
          preqs.append("](");
          preqs.append(reqD.threadDesc);
          preqs.append(") is pending.\n");
          preqs.append(rs.toString());
          preqs.append("</pre>");
          pending++;
        }
      }
      os.println("<h2>" + pending + " Pending Request(s)</h2>");
      os.println(preqs.toString());
    }
  }

  /***************************************************************************/

  /**
   * ************************************************************************
   * This is a bit of instrumentation that I kept around to let me look at the
   * state of the incoming <code>HttpServletRequest</code> from the client.
   * This method calls the <code>get*</code> methods of the request and prints
   * the results to standard out.
   *
   * @param ps The <code>PrintStream</code> to send output.
   * @param rs The <code>ReqState</code> object to probe.
   */

  public void probeRequest(PrintWriter ps, ReqState rs) {

    Enumeration e;
    int i;


    ps.println("####################### PROBE ##################################");
    ps.println("The HttpServletRequest object is actually a: " + rs.getRequest().getClass().getName());
    ps.println("");
    ps.println("HttpServletRequest Interface:");
    ps.println("    getAuthType:           " + rs.getRequest().getAuthType());
    ps.println("    getMethod:             " + rs.getRequest().getMethod());
    ps.println("    getPathInfo:           " + rs.getRequest().getPathInfo());
    ps.println("    getPathTranslated:     " + rs.getRequest().getPathTranslated());
    ps.println("    getRequestURL:         " + rs.getRequest().getRequestURL());
    ps.println("    getQueryString:        " + rs.getRequest().getQueryString());
    ps.println("    getRemoteUser:         " + rs.getRequest().getRemoteUser());
    ps.println("    getRequestedSessionId: " + rs.getRequest().getRequestedSessionId());
    ps.println("    getRequestURI:         " + rs.getRequest().getRequestURI());
    ps.println("    getServletPath:        " + rs.getRequest().getServletPath());
    ps.println("    isRequestedSessionIdFromCookie: " + rs.getRequest().isRequestedSessionIdFromCookie());
    ps.println("    isRequestedSessionIdValid:      " + rs.getRequest().isRequestedSessionIdValid());
    ps.println("    isRequestedSessionIdFromURL:    " + rs.getRequest().isRequestedSessionIdFromURL());

    ps.println("");
    i = 0;
    e = rs.getRequest().getHeaderNames();
    ps.println("    Header Names:");
    while (e.hasMoreElements()) {
      i++;
      String s = (String) e.nextElement();
      ps.print("        Header[" + i + "]: " + s);
      ps.println(": " + rs.getRequest().getHeader(s));
    }

    ps.println("");
    ps.println("ServletRequest Interface:");
    ps.println("    getCharacterEncoding:  " + rs.getRequest().getCharacterEncoding());
    ps.println("    getContentType:        " + rs.getRequest().getContentType());
    ps.println("    getContentLength:      " + rs.getRequest().getContentLength());
    ps.println("    getProtocol:           " + rs.getRequest().getProtocol());
    ps.println("    getScheme:             " + rs.getRequest().getScheme());
    ps.println("    getServerName:         " + rs.getRequest().getServerName());
    ps.println("    getServerPort:         " + rs.getRequest().getServerPort());
    ps.println("    getRemoteAddr:         " + rs.getRequest().getRemoteAddr());
    ps.println("    getRemoteHost:         " + rs.getRequest().getRemoteHost());
    //ps.println("    getRealPath:           "+rs.getRequest().getRealPath());


    ps.println(".............................");
    ps.println("");
    i = 0;
    e = rs.getRequest().getAttributeNames();
    ps.println("    Attribute Names:");
    while (e.hasMoreElements()) {
      i++;
      String s = (String) e.nextElement();
      ps.print("        Attribute[" + i + "]: " + s);
      ps.println(" Type: " + rs.getRequest().getAttribute(s));
    }

    ps.println(".............................");
    ps.println("");
    i = 0;
    e = rs.getRequest().getParameterNames();
    ps.println("    Parameter Names:");
    while (e.hasMoreElements()) {
      i++;
      String s = (String) e.nextElement();
      ps.print("        Parameter[" + i + "]: " + s);
      ps.println(" Value: " + rs.getRequest().getParameter(s));
    }

    ps.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
    ps.println(" . . . . . . . . . Servlet Infomation API  . . . . . . . . . . . . . .");
    ps.println("");

    ps.println("Servlet Context:");
    ps.println("");

    i = 0;
    e = servletContext.getAttributeNames();
    ps.println("    Attribute Names:");
    while (e.hasMoreElements()) {
      i++;
      String s = (String) e.nextElement();
      ps.print("        Attribute[" + i + "]: " + s);
      ps.println(" Type: " + servletContext.getAttribute(s));
    }

    ps.println("    ServletContext.getRealPath(\".\"): " + servletContext.getRealPath("."));
    ps.println("    ServletContext.getMajorVersion(): " + servletContext.getMajorVersion());
//        ps.println("ServletContext.getMimeType():     " + sc.getMimeType());
    ps.println("    ServletContext.getMinorVersion(): " + servletContext.getMinorVersion());
//        ps.println("ServletContext.getRealPath(): " + sc.getRealPath());


    ps.println(".............................");
    ps.println("Servlet Config:");
    ps.println("");


    ServletConfig scnfg = getServletConfig();


    i = 0;
    e = scnfg.getInitParameterNames();
    ps.println("    InitParameters:");
    while (e.hasMoreElements()) {
      String p = (String) e.nextElement();
      ps.print("        InitParameter[" + i + "]: " + p);
      ps.println(" Value: " + scnfg.getInitParameter(p));
      i++;
    }
    ps.println("");
    ps.println("######################## END PROBE ###############################");
    ps.println("");


  }
  /***************************************************************************/


  /**
   * ************************************************************************
   * <p/>
   * In this (default) implementation of the getServerName() method we just get
   * the name of the servlet and pass it back. If something different is
   * required, override this method when implementing the getDDS() and
   * getServerVersion() methods.
   * <p/>
   * This is typically used by the getINFO() method to figure out if there is
   * information specific to this server residing in the info directory that
   * needs to be returned to the client as part of the .info rs.getResponse().
   *
   * @return A string containing the name of the servlet class that is running.
   */
  public String getServerName() {

    // Ascertain the name of this server.
    String servletName = this.getClass().getName();

    return (servletName);
  }


  /**
   * ***********************************************************************
   * Handles incoming requests from clients. Parses the request and determines
   * what kind of OPeNDAP response the client is requesting. If the request is
   * understood, then the appropriate handler method is called, otherwise
   * an error is returned to the client.
   * <p/>
   * This method is the entry point for <code>AbstractServlet</code>.
   *
   * @param request  The client's <code> HttpServletRequest</code> request
   *                 object.
   * @param response The server's <code> HttpServletResponse</code> response
   *                 object.
   * @see opendap.servlet.ReqState
   */
  public void doGet(HttpServletRequest request,
          HttpServletResponse response)
  {

    // setHeader("Last-Modified", (new Date()).toString() );

    boolean isDebug = false;
    ReqState rs = null;
    RequestDebug reqD = null;
    try {

      rs = getRequestState(request, response);
      if (rs != null) {
          if (Debug.isSet("probeRequest")) {
              probeRequest(new PrintWriter(new OutputStreamWriter(System.out,Util.UTF8),true), rs);
          }

          String ds = rs.getDataSet();
        String suff = rs.getRequestSuffix();
        isDebug = ((ds != null) && ds.equals("debug") && (suff != null) && suff.equals(""));
      }

      synchronized (syncLock) {

        if (!isDebug) {
          long reqno = HitCounter++;
          if (track) {
            reqD = new RequestDebug(reqno, Thread.currentThread().toString());
            rs.setUserObject(reqD);
            if (prArr == null) prArr = new ArrayList(10000);
            prArr.add((int) reqno, rs);
          }

          if (Debug.isSet("showRequest")) {
           log.debug("-------------------------------------------");
           log.debug("Server: " + getServerName() + "   Request #" + reqno);
           log.debug("Client: " + rs.getRequest().getRemoteHost());
           log.debug(rs.toString());
           log.debug("Request dataset: '" + rs.getDataSet() + "' suffix: '" + rs.getRequestSuffix() +
                    "' CE: '" + rs.getConstraintExpression() + "'");
          }
        }
      } // synch

      if (rs != null) {
        String dataSet = rs.getDataSet();
        String requestSuffix = rs.getRequestSuffix();

        if (dataSet == null) {
          doGetDIR(rs);
        } else if (dataSet.equals("/")) {
          doGetDIR(rs);
        } else if (dataSet.equals("")) {
          doGetDIR(rs);
        } else if (dataSet.equalsIgnoreCase("/version") || dataSet.equalsIgnoreCase("/version/")) {
          doGetVER(rs);
        } else if (dataSet.equalsIgnoreCase("/help") || dataSet.equalsIgnoreCase("/help/")) {
          doGetHELP(rs);
        } else if (dataSet.equalsIgnoreCase("/" + requestSuffix)) {
          doGetHELP(rs);
        } else if (requestSuffix.equalsIgnoreCase("dds")) {
          doGetDDS(rs);
        } else if (requestSuffix.equalsIgnoreCase("das")) {
          doGetDAS(rs);
        } else if (requestSuffix.equalsIgnoreCase("ddx")) {
          doGetDDX(rs);
        } else if (requestSuffix.equalsIgnoreCase("blob")) {
          doGetBLOB(rs);
        } else if (requestSuffix.equalsIgnoreCase("dods")) {
          doGetDAP2Data(rs);
        } else if (requestSuffix.equalsIgnoreCase("asc") ||
                requestSuffix.equalsIgnoreCase("ascii")) {
          doGetASC(rs);
        } else if (requestSuffix.equalsIgnoreCase("info")) {
          doGetINFO(rs);
        } else if (requestSuffix.equalsIgnoreCase("html") || requestSuffix.equalsIgnoreCase("htm")) {
          doGetHTML(rs);
        } else if (requestSuffix.equalsIgnoreCase("ver") || requestSuffix.equalsIgnoreCase("version")) {
          doGetVER(rs);
        } else if (requestSuffix.equalsIgnoreCase("help")) {
          doGetHELP(rs);

        /* JC added
        } else if (dataSet.equalsIgnoreCase("catalog") && requestSuffix.equalsIgnoreCase("xml")) {
          doGetCatalog(rs);
        } else if (dataSet.equalsIgnoreCase("status")) {
          doGetStatus(rs);
        } else if (dataSet.equalsIgnoreCase("systemproperties")) {
          doGetSystemProps(rs);
        } else if (isDebug) {
          doDebug(rs);  */
        } else if (requestSuffix.equals("")) {
          badURL(rs);
        } else {
          badURL(rs);
        }
      } else {
        badURL(rs);
      }

      if (reqD != null) reqD.done = true;
    } catch (Throwable e) {
      anyExceptionHandler(e, rs);
    }

  }

  /**
   * @param request
   * @return the request state
   */
  protected ReqState
  getRequestState(HttpServletRequest request, HttpServletResponse response)
    throws DAP2Exception
  {
      ReqState rs = null;
      // The url and query strings will come to us in encoded form
      // (see HTTPmethod.newMethod())
      String baseurl = request.getRequestURL().toString();
      baseurl = EscapeStrings.unescapeURL(baseurl);

      String query = request.getQueryString();
      query = EscapeStrings.unescapeURLQuery(query);
      rs = new ReqState(request, response, this, getServerName(), baseurl, query);
      return rs;
  }
  //**************************************************************************

  void showMemUsed(String from) {
    long totalMemory = Runtime.getRuntime().totalMemory();
    long freeMemory = Runtime.getRuntime().freeMemory();
    //long maxMemory = Runtime.getRuntime ().maxMemory ();
    long usedMemory = (totalMemory - freeMemory);

   log.debug("****showMemUsed " + from);
   log.debug(" totalMemory " + totalMemory);
   log.debug(" freeMemory " + freeMemory);
    //LogStream.out.println(" maxMemory "+maxMemory);
   log.debug(" usedMemory " + usedMemory);
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
    pw.println("http://opendap.gso.url.edu/cgi-bin/nph-nc/data/fnoc1.nc.das.");

    pw.println("<p><b>Note</b>: Many OPeNDAP clients supply these extensions for you so you don't");
    pw.println("need to append them (for example when using interfaces supplied by us or");
    pw.println("software re-linked with a OPeNDAP client-library). Generally, you only need to");
    pw.println("add these if you are typing a URL directly into a WWW browser.");
    pw.println("<p><b>Note</b>: If you would like version information for this server but");
    pw.println("don't know a specific data file or data set name, use `/version' for the");
    pw.println("filename. For example: http://opendap.gso.url.edu/cgi-bin/nph-nc/version will");
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

    pw.println("<h3>Error in URL</h3>");
    pw.println("The URL extension did not match any that are known by this");
    pw.println("server. Below is a list of the five extensions that are be recognized by");
    pw.println("all OPeNDAP servers. If you think that the server is broken (that the URL you");
    pw.println("submitted should have worked), then please contact the");
    pw.println("OPeNDAP user support coordinator at: ");
    pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a><p>");

  }
  //**************************************************************************

  // debug
  private ArrayList prArr = null;

  static private class RequestDebug {
    long reqno;
    String threadDesc;
    boolean done = false;

    RequestDebug(long reqno, String threadDesc) {
      this.reqno = reqno;
      this.threadDesc = threadDesc;
    }
  }
}





