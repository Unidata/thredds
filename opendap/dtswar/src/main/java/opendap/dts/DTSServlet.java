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


package opendap.dts;

import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import javax.servlet.*;
import javax.servlet.http.*;

import opendap.dap.*;
import opendap.servers.*;
import opendap.dap.parsers.ParseException;
import opendap.servlet.*;
import opendap.util.Debug;
import ucar.nc2.util.EscapeStrings;

/**
 * DTSServlet is a servlet to support client testing.
 *
 * Default handlers for all of the acceptable OPeNDAP client
 * requests are here.
 * <p/>
 * Each of the request handlers appears as an adjunct method to
 * the doGet() method of the base servlet class. In order to
 * reduce the bulk of this file, many of these methods have been
 * in wrapper classes in this package (opendap.servlet).
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
 *
 * @author Nathan David Potter
 * @author jcaron 2/7/07 merge changes
 * @author dheimbigner 7/29/11 refactor
 * @see opendap.servlet.GetAsciiHandler
 * @see opendap.servlet.GetDirHandler
 * @see opendap.servlet.GetHTMLInterfaceHandler
 * @see opendap.servlet.GetInfoHandler
 * @see opendap.servlet.ReqState
 * @see opendap.servlet.ParsedRequest
 * @see opendap.servlet.GuardedDataset
 */


/**
 * <b>Purpose:</b><br>
 * This is the OPeNDAP Test servlet (dts). It allows the owner of the server
 * to deliver data in ANY valid DDS to a client. This DDS will be
 * filled with invented data if the client requests a DataDDS.
 * This kind of test fixture is useful for evaluating a clients
 * ability to handle the various complexities of the OPeNDAP data
 * types.
 * <p/>
 * <b>Configuration:</b><br>
 * The AbstractServlet relies on the javax.servlet.ServletConfig
 * interface (in particular the getInitParameter() method)
 * to retrieve configuration information used by the servlet.
 * <b>InitParameters:</b>
 * <p/>
 * <ul>
 * <li>
 * DebugOn - This controls ouput to the terminal from which
 * the servlet engine was launched. The value is a list of
 * flags that turn on debugging instrumentation in different
 * parts of the code. Values are:
 * <ul>
 * <li>showRequest  - Show information about the clients request. </li>
 * <li>showResponse - Show information about the servlets response.</li>
 * <li>probeRequest - Show an exhaustive amount of information about
 * the clients request object.</li>
 * </ul>
 * </li><br>
 * <p/>
 * <li>
 * INFOcache - This is should be set to the directory containing the
 * files used by the ".info" service for the servlet. This directory
 * should contain any dataset specific "over-ride" files (see below),
 * any dataset specific additional information files (see below), and any
 * servlet specific information files(see below).
 * </li><br>
 * <p/>
 * <li>
 * DDScache - This is should be set to the directory containing the DDS
 * files for the datasets used by the servlet. Some servlets have been
 * developed that do not use DDS's that are cached on the disk, however
 * the default behaviour is for the servlet to load DDS images from disk.
 * </li><br>
 * <p/>
 * <li>
 * DAScache - This is should be set to the directory containing the DAS
 * files for the datasets used by the servlet. Some servlets have been
 * developed that do not use DAS's that are cached on the disk, however
 * the default behaviour is for the servlet to load DAS images from disk.
 * </li><br>
 * <p/>
 * </ul>
 * Here is an example entry from the web.xml file (for tomcat3.3a) for
 * the OPeNDAP Test Server (DTS):
 * <p/>
 * <pre>
 *         &lt;servlet&gt;
 *            &lt;servlet-name&gt;
 *                dts
 *            &lt;/servlet-name&gt;
 *
 *            &lt;servlet-class&gt;
 *                opendap.servers.dts
 *            &lt;/servlet-class&gt;
 *
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;DebugOn&lt;/param-name&gt;
 *                &lt;param-value&gt;showRequest showResponse &lt;/param-value&gt;
 *            &lt;/init-param&gt;
 *
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;INFOcache&lt;/param-name&gt;
 *                &lt;param-value&gt;/usr/Java-OPeNDAP/sdds-testsuite/info/&lt;/param-value&gt;
 *            &lt;/init-param&gt;
 *
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;DDScache&lt;/param-name&gt;
 *                &lt;param-value&gt;/usr/Java-OPeNDAP/sdds-testsuite/dds/&lt;/param-value&gt;
 *            &lt;/init-param&gt;
 *
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;DAScache&lt;/param-name&gt;
 *                &lt;param-value&gt;/usr/Java-OPeNDAP/sdds-testsuite/das/&lt;/param-value&gt;
 *            &lt;/init-param&gt;
 *        &lt;/servlet&gt;
 * </pre>
 *
 * @author Nathan David Potter
 * @version $Revision: 16122 $
 */

public class DTSServlet extends AbstractServlet
{
    static final boolean debug = false;

    static public org.slf4j.Logger log
        = org.slf4j.LoggerFactory.getLogger(DTSServlet.class);

    // Class variables
    static final String DEFAULTCONTEXTPATH = "/dts";

    //static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DODSServlet.class);

    private static FunctionLibrary functionLibrary =
        new FunctionLibrary("opendap.servers.SSF");

    // Instance variables

    /**
     * ************************************************************************
     * OPeNDAP Server Version for the test server...
     *
     * @serial Ostensibly it's a serializable item...
     */

    private String ServerVersion = "DODS/3.2";

    /**
     * Debugging
     */
    private boolean track = false;


    /**
     * Used for thread syncronization.
     *
     * @serial
     */
    private Object syncLock = new Object();


    /**
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

    /**
     * Getter function for rootpath
     *
     * @return rootpath
     */
    public String getRootPath()
    {
        return rootpath;
    }

    /**
     * This method returns a String containing the OPeNDAP Server Version...
     */
    public String getServerVersion()
    {
        return (ServerVersion);
    }


    /**
     * ************************************************************************
     * This method must be implemented locally for each OPeNDAP server. The
     * local implementation of this method is the key piece for connecting
     * any localized data types that are derived from the opendap.Server types
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
     */
    protected GuardedDataset getDataset(ReqState rs)
        throws DAP2Exception, IOException, ParseException
    {
        return new testDataset(rs);
    }

    private void cacheArrayShapes(ServerDDS sdds)
    {
        Enumeration e = sdds.getVariables();
        while(e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();
            cAS(bt);
        }
    }

    private void cAS(BaseType bt)
    {
        if(bt instanceof DConstructor) {
            Enumeration e = ((DConstructor) bt).getVariables();
            while(e.hasMoreElements()) {
                BaseType tbt = (BaseType) e.nextElement();
                cAS(tbt);
            }
        } else if(bt instanceof test_SDArray) {
            ((test_SDArray) bt).cacheShape();
        }
    }


    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from anywhere in the VM.
     */

    public DTSServlet()
    {
    }

    public void init() throws ServletException
    {
        super.init();
        setLog(DTSLog.class);
        log.debug("**************** DTS INIT ***********************");

        // debugging
        String debugOn = getInitParameter("DebugOn");
        if(debugOn != null) {
            StringTokenizer toker = new StringTokenizer(debugOn);
            while(toker.hasMoreTokens())
                Debug.set(toker.nextToken(), true);
        }

        servletConfig = this.getServletConfig();
        servletContext = servletConfig.getServletContext();
        rootpath = servletContext.getRealPath("/");
        log.debug("rootpath=" + rootpath);
    }
    /***************************************************************************/


    /**
     * Turns a ParseException into a OPeNDAP DAP2 error and sends it to the client.
     *
     * @param pe       The <code>ParseException</code> that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public void parseExceptionHandler(ParseException pe, HttpServletResponse response)
    {
        //log.error("DODSServlet.parseExceptionHandler", pe);

        if(Debug.isSet("showException")) {
            log.error(pe.toString());
            printThrowable(pe);
        }

        try {
            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
            response.setHeader("Content-Description", "dods-error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");
            // response.setContentType("text/plain");

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

    /**
     * Sends a OPeNDAP DAP2 error to the client.
     *
     * @param de       The OPeNDAP DAP2 exception that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public void dap2ExceptionHandler(DAP2Exception de, HttpServletResponse response)
    {
        //log.info("DODSServlet.dodsExceptionHandler (" + de.getErrorCode() + ") " + de.getErrorMessage());

        if(Debug.isSet("showException")) {
            log.error(de.toString());
            de.printStackTrace();
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

    /**
     * Sends an error to the client.
     * fix: The problem is that if the message is already committed when the IOException occurs, the headers dont get set.
     *
     * @param e  The exception that caused the problem.
     * @param rs The <code>ReqState</code> for the client.
     */
    public void IOExceptionHandler(IOException e, ReqState rs)
    {
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
            if(msg != null)
                msg = msg.replace('\"', '\'');

            DAP2Exception de2 = new DAP2Exception(opendap.dap.DAP2Exception.CANNOT_READ_FILE, msg);
            de2.print(eOut);

            if(Debug.isSet("showException")) { // Error message
                log.error("DODServlet ERROR (IOExceptionHandler): " + e);
                log.error(rs.toString());
                if(track) {
                    RequestDebug reqD = (RequestDebug) rs.getUserObject();
                    log.error("  request number: " + reqD.reqno + " thread: " + reqD.threadDesc);
                }
                printThrowable(e);
            }

        } catch (IOException ioe) {
            log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
        }

    }

    /**
     * Sends an error to the client.
     *
     * @param e  The exception that caused the problem.
     * @param rs The <code>ReqState</code> for the client.
     */

    public void anyExceptionHandler(Throwable e, ReqState rs)
    {
        try {
            log.error("DODServlet ERROR (anyExceptionHandler): " + e);
            printThrowable(e);
            // Strip any double quotes out of the parser error message.
            // These get stuck in auto-magically by the javacc generated parser
            // code and they break our error parser (bummer!)
            String msg = e.getMessage();
            if(msg != null)
                msg = msg.replace('\"', '\'');

            if(rs != null) {
                HttpServletResponse response = rs.getResponse();
                log.error(rs + "");
                if(track) {
                    RequestDebug reqD = (RequestDebug) rs.getUserObject();
                    log.error("  request number: " + reqD.reqno + " thread: " + reqD.threadDesc);
                }
                BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
                response.setHeader("Content-Description", "dods-error");

                // This should probably be set to "plain" but this works, the
                // C++ slients don't barf as they would if I sent "plain" AND
                // the C++ don't expect compressed data if I do this...
                response.setHeader("Content-Encoding", "");

                DAP2Exception de2 = new DAP2Exception(opendap.dap.DAP2Exception.UNDEFINED_ERROR, msg);
                de2.print(eOut);
            }

        } catch (IOException ioe) {
            log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
        }

    }


    /**
     * Sends a OPeNDAP DAP2 error (type UNKNOWN ERROR) to the client and displays a
     * message on the server console.
     *
     * @param rs        The client's <code> ReqState</code> object.
     * @param clientMsg Error message <code>String</code> to send to the client.
     * @param serverMsg Error message <code>String</code> to display on the server console.
     */
    public void sendDODSError(ReqState rs,
                              String clientMsg,
                              String serverMsg)
        throws Exception
    {
        rs.getResponse().setContentType("text/plain");
        rs.getResponse().setHeader("XDODS-Server", getServerVersion());
        rs.getResponse().setHeader("Content-Description", "dods-error");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "none");

        ServletOutputStream Out = rs.getResponse().getOutputStream();

        DAP2Exception de = new DAP2Exception(opendap.dap.DAP2Exception.UNKNOWN_ERROR, clientMsg);

        de.print(Out);

        rs.getResponse().setStatus(HttpServletResponse.SC_OK);

        log.error(serverMsg);

    }


    /**
     * Handler for the client's DAS request. Operates on the assumption
     * that the DAS information is cached on a disk local to the server. If you
     * don't like that, then you better override it in your server :)
     * <p/>
     * <p>Once the DAS has been parsed it is sent to the requesting client.
     *
     * @param rs The ReqState of this client request. Contains all kinds of
     *           important stuff.
     * @see ReqState
     */
    public void doGetDAS(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.error("doGetDAS for dataset: " + rs.getDataSet());
        }

        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(ds == null) return;

            rs.getResponse().setContentType("text/plain");
            rs.getResponse().setHeader("XDODS-Server", getServerVersion());
            rs.getResponse().setHeader("Content-Description", "dods-das");
            // Commented because of a bug in the OPeNDAP C++ stuff...
            //rs.getResponse().setHeader("Content-Encoding", "plain");

            OutputStream Out = new BufferedOutputStream(rs.getResponse().getOutputStream());

            DAS myDAS = ds.getDAS();
            rs.getResponse().setStatus(HttpServletResponse.SC_OK);
            myDAS.print(Out);
            if(Debug.isSet("showResponse")) {
//        log.debug("DAS=\n");
//        myDAS.print(System.out);
            }

        } catch (DAP2Exception de) {
            dap2ExceptionHandler(de, rs.getResponse());
        } catch (ParseException pe) {
            parseExceptionHandler(pe, rs.getResponse());
        } catch (Throwable t) {
            anyExceptionHandler(t, rs);
        } finally { // release lock if needed
            if(ds != null) ds.release();
        }

    }


    /**
     * Handler for the client's DDS request. Requires the getDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param rs The ReqState of this client request. Contains all kinds of
     *           important stuff.
     * @see ReqState
     */
    public void doGetDDS(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("doGetDDS for dataset: " + rs.getDataSet());
        }

        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(null == ds) return;

            rs.getResponse().setContentType("text/plain");
            rs.getResponse().setHeader("XDODS-Server", getServerVersion());
            rs.getResponse().setHeader("Content-Description", "dods-dds");
            // Commented because of a bug in the OPeNDAP C++ stuff...
            //rs.getResponse().setHeader("Content-Encoding", "plain");

            OutputStream Out = new BufferedOutputStream(rs.getResponse().getOutputStream());

            // Utilize the getDDS() method to get a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = ds.getDDS();

            if(rs.getConstraintExpression().equals("")) { // No Constraint Expression?
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
            if(Debug.isSet("showResponse")) {
                if(rs.getConstraintExpression().equals("")) { // No Constraint Expression?
                    log.debug("Unconstrained DDS=\n");
                    // myDDS.print();
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
            if(ds != null) ds.release();
        }

    }

    /**
     * Handler for the client's DDX request. Requires the getDDX() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDX has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param rs The ReqState of this client request. Contains all kinds of
     *           important stuff.
     * @see ReqState
     */
    public void doGetDDX(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("doGetDDX for dataset: " + rs.getDataSet());
        }

        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(null == ds) return;

            rs.getResponse().setContentType("text/plain");
            rs.getResponse().setHeader("XDODS-Server", getServerVersion());
            rs.getResponse().setHeader("Content-Description", "dods-ddx");
            // Commented because of a bug in the OPeNDAP C++ stuff...
            // rs.getResponse().setHeader("Content-Encoding", "plain");

            OutputStream Out = new BufferedOutputStream(rs.getResponse().getOutputStream());

            // Utilize the getDDS() method to get a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = ds.getDDS();

            if(rs.getConstraintExpression().equals("")) { // No Constraint Expression?
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
            if(Debug.isSet("showResponse")) {
                if(rs.getConstraintExpression().equals("")) { // No Constraint Expression?
//          log.debug("Unconstrained DDX=\n");
//          myDDS.printXML(System.out);
                } else {
//          log.debug("Constrained DDX=\n");
//          myDDS.printConstrainedXML(System.out);
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
            if(ds != null) ds.release();
        }

    }

    /**
     * Handler for the client's data request. Requires the getDDS()
     * method implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed, the data is read (using the class in the
     * localized server factory etc.), compared to the constraint expression,
     * and then sent to the client.
     *
     * @param rs The ReqState of this client request. Contains all kinds of
     *           important stuff.
     * @opendap.ddx.experimental
     * @see ReqState
     */
    public void doGetBLOB(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("doGetBLOB For: " + rs.getDataSet());
        }
        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(ds == null) return;

            rs.getResponse().setContentType("application/octet-stream");
            rs.getResponse().setHeader("XDODS-Server", getServerVersion());
            rs.getResponse().setHeader("Content-Description", "dods-blob");

            ServletOutputStream sOut = rs.getResponse().getOutputStream();
            OutputStream bOut;
            DeflaterOutputStream dOut = null;

            if(rs.getAcceptsCompressed() && allowDeflate) {
                rs.getResponse().setHeader("Content-Encoding", "deflate");
                dOut = new DeflaterOutputStream(sOut);
                bOut = new BufferedOutputStream(dOut);
            } else {
                // Commented out because of a bug in the OPeNDAP C++ stuff...
                //rs.getResponse().setHeader("Content-Encoding", "plain");
                bOut = new BufferedOutputStream(sOut);
            }

            // Utilize the getDDS() method to get
            // a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = ds.getDDS();
            cacheArrayShapes(myDDS);

            // Instantiate the CEEvaluator and parse the constraint expression
            CEEvaluator ce = new CEEvaluator(myDDS,
                new ClauseFactory(functionLibrary));
            ce.parseConstraint(rs.getConstraintExpression(), rs.getRequestURL().toString());

            // Send the binary data back to the client
            DataOutputStream sink = new DataOutputStream(bOut);

            int seqLength = 5;
            String sls = rs.getInitParameter("SequenceLength");
            if(sls != null) {
                seqLength = (Integer.valueOf(sls)).intValue();
            }

            testEngine te = new testEngine(seqLength);

            ce.send(myDDS.getEncodedName(), sink, te);
            sink.flush();

            // Finish up sending the compressed stuff, but don't
            // close the stream (who knows what the Servlet may expect!)
            if(rs.getAcceptsCompressed()) {
                if(bOut != null)
                    bOut.flush();
                if(dOut != null)
                    ((DeflaterOutputStream) dOut).finish();
            }

            //? if(null != dOut) dOut.finish();
            //? bOut.flush();

            rs.getResponse().setStatus(HttpServletResponse.SC_OK);

        } catch (ParseException pe) {
            parseExceptionHandler(pe, rs.getResponse());
        } catch (DAP2Exception de) {
            dap2ExceptionHandler(de, rs.getResponse());
        } catch (IOException ioe) {
            IOExceptionHandler(ioe, rs);
        } finally {  // release lock if needed
            if(ds != null) ds.release();
        }

    }


    /**
     * Handler for the client's data request. Requires the getDDS()
     * method implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed, the data is read (using the class in the
     * localized server factory etc.), compared to the constraint expression,
     * and then sent to the client.
     *
     * @param rs The ReqState of this client request. Contains all kinds of
     *           important stuff.
     * @throws IOException
     * @throws ServletException
     * @see ReqState
     */
    public void doGetDAP2Data(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("doGetDAP2Data For: " + rs.getDataSet());
        }
        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(ds == null) return;

            rs.getResponse().setContentType("application/octet-stream");
            rs.getResponse().setHeader("XDODS-Server", getServerVersion());
            rs.getResponse().setHeader("Content-Description", "dods-data");
            rs.getResponse().setStatus(HttpServletResponse.SC_OK);

            ServletOutputStream sOut = rs.getResponse().getOutputStream();
            OutputStream bOut;
            DeflaterOutputStream dOut = null;
            if(rs.getAcceptsCompressed() && allowDeflate) {
                rs.getResponse().setHeader("Content-Encoding", "deflate");
                dOut = new DeflaterOutputStream(sOut);
                bOut = new BufferedOutputStream(dOut);
            } else {
                // Commented out because of a bug in the OPeNDAP C++ stuff...
                //rs.getResponse().setHeader("Content-Encoding", "plain");
                bOut = new BufferedOutputStream(sOut);
            }

            // Utilize the getDDS() method to get
            // a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = ds.getDDS();

            cacheArrayShapes(myDDS);

            // Instantiate the CEEvaluator and parse the constraint expression
            CEEvaluator ce = new CEEvaluator(myDDS, new ClauseFactory(functionLibrary));
            // and parse the constraint expression
            ce.parseConstraint(rs);

            // debug
            // log.debug("CE DDS = ");
            // myDDS.printConstrained(System.out);

            // Send the constrained DDS back to the client
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(bOut,Util.UTF8));
            myDDS.printConstrained(pw);

            // Send the Data delimiter back to the client
            //pw.println("Data:"); // JCARON CHANGED
            pw.flush();
            bOut.write("\nData:\r\n".getBytes("UTF-8")); // JCARON CHANGED
            bOut.flush();

            int seqLength = 5;
            String sls = rs.getInitParameter("SequenceLength");
            if(sls != null) {
                seqLength = Integer.valueOf(sls);
            }

            // Send the binary data back to the client
            DataOutputStream sink = new DataOutputStream(bOut);
            testEngine te = new testEngine(seqLength);
            ce.send(myDDS.getEncodedName(), sink, te);
            sink.flush();

            // Finish up sending the compressed stuff, but don't
            // close the stream (who knows what the Servlet may expect!)
            if(rs.getAcceptsCompressed() && allowDeflate) {
                bOut.flush();
                dOut.finish();
                // was ((DeflaterOutputStream) bOut).finish();  causes casting error
            }

            //? if(dOut != null) dOut.finish();
            //? bOut.flush();

            //? rs.getResponse().setStatus(HttpServletResponse.SC_OK);

        } catch (ParseException pe) {
            parseExceptionHandler(pe, rs.getResponse());
        } catch (DAP2Exception de) {
            dap2ExceptionHandler(de, rs.getResponse());
        } catch (IOException ioe) {
            IOExceptionHandler(ioe, rs);
        } finally {  // release lock if needed
            if(ds != null) ds.release();
        }

    }

    /**
     * Handler for the client's directory request.
     * <p/>
     * Returns an html document to the client showing (a possibly pseudo)
     * listing of the datasets available on the server in a directory listing
     * format.
     * <p/>
     * The bulk of this code resides in the class opendap.servlet.GetDirHandler and
     * documentation may be found there.
     *
     * @param rs The client's <code> ReqState</code>
     * @see opendap.servlet.GetDirHandler
     */
    public void doGetDIR(ReqState rs)
        throws Exception
    {
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

    /**
     * Handler for the client's version request.
     * <p/>
     * <p>Returns a plain text document with server version and OPeNDAP core
     * version #'s
     *
     * @param rs The client's <code> ReqState</code>
     */
    public void doGetVER(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
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


    /**
     * Handler for the client's help request.
     * <p/>
     * <p> Returns an html page of help info for the server
     *
     * @param rs The client's <code> ReqState </code>
     */
    public void doGetHELP(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
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


    /**
     * Sends an html document to the client explaining that they have used a
     * poorly formed URL and then the help page...
     *
     * @param request  The client's <code> request </code>
     * @param response The client <code>response</code>
     */
    public void badURL(HttpServletRequest request, HttpServletResponse response)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("Sending Bad URL Page.");
        }

        //log.info("DODSServlet.badURL " + rs.getRequest().getRequestURI());

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getServerVersion());
        response.setHeader("Content-Description", "dods-error");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //rs.getResponse().setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(),Util.UTF8));

        printBadURLPage(pw);
        printHelpPage(pw);
        pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);

    }

    /**
     * Handler for OPeNDAP ascii data requests. Returns the request data as
     * a comma delimited ascii file. Note that this means that the more complex
     * OPeNDAP structures such as Grids get flattened...
     * <p/>
     * <p/>
     * Modified 2/8/07 jcaron to not make a DConnect2 call to itself
     *
     * @param rs the decoded Request State
     */
    public void doGetASC(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("doGetASC For: " + rs.getDataSet());
        }

        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(ds == null) return;

            rs.getResponse().setHeader("XDODS-Server", getServerVersion());
            rs.getResponse().setContentType("text/plain");
            rs.getResponse().setHeader("Content-Description", "dods-ascii");
            rs.getResponse().setStatus(HttpServletResponse.SC_OK);

            if(debug)
                log.debug("Sending OPeNDAP ASCII Data For: " + rs + "  CE: '" + rs.getConstraintExpression() + "'");

            ServerDDS dds = ds.getDDS();
            cacheArrayShapes(dds);

            // Instantiate the CEEvaluator and parse the constraint expression
            CEEvaluator ce = new CEEvaluator(dds,
                new ClauseFactory(functionLibrary));

            // and parse the constraint expression
            ce.parseConstraint(rs);

            int seqLength = 5;
            String sls = rs.getInitParameter("SequenceLength");
            if(sls != null) {
                seqLength = Integer.valueOf(sls);
            }
            log.debug("Sequence Length: " + seqLength);

            testEngine te = new testEngine(seqLength);

            PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
            dds.printConstrained(pw);
            pw.println("---------------------------------------------");

            AsciiWriter writer = new AsciiWriter(); // could be static
            writer.toASCII(pw, dds, te);
            pw.flush();

            log.debug("ASCII Response Sent");

            // the way that getDAP2Data works
            // DataOutputStream sink = new DataOutputStream(bOut);
            // ce.send(myDDS.getName(), sink, ds);
            // rs.getResponse().setStatus(HttpServletResponse.SC_OK);

        } catch (ParseException pe) {
            parseExceptionHandler(pe, rs.getResponse());
        } catch (DAP2Exception de) {
            dap2ExceptionHandler(de, rs.getResponse());
        } catch (Throwable t) {
            anyExceptionHandler(t, rs);
        } finally { // release lock if needed
            if(ds != null) ds.release();
        }

    }

    /**
     * Handler for OPeNDAP info requests. Returns an HTML document
     * describing the contents of the servers datasets.
     * <p/>
     * The bulk of this code resides in the class opendap.servlet.GetInfoHandler and
     * documentation may be found there.
     *
     * @param rs The client's <code> ReqState </code>
     * @see GetInfoHandler
     */
    public void doGetINFO(ReqState rs)
        throws Exception
    {
        if(Debug.isSet("showResponse")) {
            log.debug("doGetINFO For: " + rs.getDataSet());
        }

        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(ds == null) return;

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
            if(ds != null) ds.release();
        }

    }

    /**
     * Handler for OPeNDAP .html requests. Returns the OPeNDAP Web
     * Interface (aka The Interface From Hell) to the client.
     * <p/>
     * The bulk of this code resides in the class
     * opendap.servlet.GetHTMLInterfaceHandler and
     * documentation may be found there.
     *
     * @param rs The client's <code> ReqState</code>
     * @see GetHTMLInterfaceHandler
     */

    public void doGetHTML(ReqState rs)
        throws Exception
    {
        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);
            if(ds == null) return;

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
            if(ds != null) ds.release();
        }

    }

    /**
     * Handler for OPeNDAP catalog.xml requests.
     *
     * @param rs The client's <code> ReqState </code>
     * @see GetHTMLInterfaceHandler
     */

    public void doGetCatalog(ReqState rs)
        throws Exception
    {
        rs.getResponse().setHeader("XDODS-Server", getServerVersion());
        rs.getResponse().setContentType("text/xml");
        rs.getResponse().setHeader("Content-Description", "dods-catalog");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
        printCatalog(rs, pw);
        pw.flush();
        rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    }

    // to be overridden by servers that implement catalogs
    protected void printCatalog(ReqState rs, PrintWriter os)
        throws IOException
    {
        os.println("Catalog not available for this server");
        os.println("Server version = " + getServerVersion());
    }

    /**
     * Handler for debug requests;
     *
     * @param rs The client's <code> ReqState </code>  object.
     */
    public void doDebug(ReqState rs)
    {
        rs.getResponse().setHeader("XDODS-Server", getServerVersion());
        rs.getResponse().setContentType("text/html");
        rs.getResponse().setHeader("Content-Description", "dods_debug");

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
        } catch (IOException e) {
            return;
        }
        pw.println("<title>Debugging</title>");
        pw.println("<body><pre>");

        StringTokenizer tz = new StringTokenizer(rs.getConstraintExpression(), "=;");
        while(tz.hasMoreTokens()) {
            String cmd = tz.nextToken();
            pw.println("Cmd= " + cmd);

            if(cmd.equals("help")) {
                pw.println(" help;log;logEnd;logShow");
                pw.println(" showFlags;showInitParameters;showRequest");
                pw.println(" on|off=(flagName)");
                doDebugCmd(cmd, tz, pw); // for subclasses
            } else if(cmd.equals("on"))
                Debug.set(tz.nextToken(), true);

            else if(cmd.equals("off"))
                Debug.set(tz.nextToken(), false);

            else if(cmd.equals("showFlags")) {
                Iterator iter = Debug.keySet().iterator();
                while(iter.hasNext()) {
                    String key = (String) iter.next();
                    pw.println("  " + key + " " + Debug.isSet(key));
                }
            } else if(cmd.equals("showInitParameters"))
                pw.println(rs.toString());

            else if(cmd.equals("showRequest"))
                probeRequest(pw, rs);

            else if(!doDebugCmd(cmd, tz, pw)) { // for subclasses
                pw.println("  unrecognized command");
            }
        }

        pw.println("--------------------------------------");
        pw.println("Logging is on");
        Iterator iter = Debug.keySet().iterator();
        while(iter.hasNext()) {
            String key = (String) iter.next();
            boolean val = Debug.isSet(key);
            if(val)
                pw.println("  " + key + " " + Debug.isSet(key));
        }

        pw.println("</pre></body>");
        pw.flush();
        rs.getResponse().setStatus(HttpServletResponse.SC_OK);

    }

    protected boolean doDebugCmd(String cmd, StringTokenizer tz, PrintStream pw)
    {
        return false;
    }

    /**
     * Handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param rs The client's <code> ReqState </code>
     * @see GetHTMLInterfaceHandler
     */
    public void doGetSystemProps(ReqState rs)
        throws Exception
    {
        rs.getResponse().setHeader("XDODS-Server", getServerVersion());
        rs.getResponse().setContentType("text/html");
        rs.getResponse().setHeader("Content-Description", "dods-status");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));
        pw.println("<html>");
        pw.println("<title>System Properties</title>");
        pw.println("<hr>");
        pw.println("<body><h2>System Properties</h2>");
        pw.println("<h3>Date: " + new Date() + "</h3>");

        Properties sysp = System.getProperties();
        Enumeration e = sysp.propertyNames();

        pw.println("<ul>");
        while(e.hasMoreElements()) {
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
     * Handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param rs The client's <code> ReqState</code>
     * @see GetHTMLInterfaceHandler
     */
    public void doGetStatus(ReqState rs)
        throws Exception
    {
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
    protected void printStatus(PrintWriter os)
    {
        os.println("<h2>Server version = " + getServerVersion() + "</h2>");
        os.println("<h2>Number of Requests Received = " + HitCounter + "</h2>");
        if(track) {
            int n = prArr.size();
            int pending = 0;
            StringBuilder preqs = new StringBuilder();
            for(int i = 0;i < n;i++) {
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

    /**
     * This is a bit of instrumentation that I kept around to let me look at the
     * state of the incoming <code>HttpServletRequest</code> from the client.
     * This method calls the <code>get*</code> methods of the request and prints
     * the results to standard out.
     *
     * @param ps The <code>PrintStream</code> to send output.
     * @param rs The <code>ReqState</code> object to probe.
     */

    public void probeRequest(PrintStream ps, ReqState rs)
    {
        Enumeration e;
        int i;
        HttpServletRequest request = rs.getRequest();

        ps.println("####################### PROBE ##################################");
        ps.println("The HttpServletRequest object is actually a: " + request.getClass().getName());
        ps.println("");
        ps.println("HttpServletRequest Interface:");
        ps.println("    getAuthType:           " + request.getAuthType());
        ps.println("    getMethod:             " + request.getMethod());
        ps.println("    getPathInfo:           " + request.getPathInfo());
        ps.println("    getPathTranslated:     " + request.getPathTranslated());
        ps.println("    getRequestURL:         " + request.getRequestURL());
        ps.println("    getQueryString:        " + request.getQueryString());
        ps.println("    getRemoteUser:         " + request.getRemoteUser());
        ps.println("    getRequestedSessionId: " + request.getRequestedSessionId());
        ps.println("    getRequestURI:         " + request.getRequestURI());
        ps.println("    getServletPath:        " + request.getServletPath());
        ps.println("    isRequestedSessionIdFromCookie: " + request.isRequestedSessionIdFromCookie());
        ps.println("    isRequestedSessionIdValid:      " + request.isRequestedSessionIdValid());
        ps.println("    isRequestedSessionIdFromURL:    " + request.isRequestedSessionIdFromURL());

        ps.println("");
        i = 0;
        e = request.getHeaderNames();
        ps.println("    Header Names:");
        while(e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Header[" + i + "]: " + s);
            ps.println(": " + request.getHeader(s));
        }

        ps.println("");
        ps.println("ServletRequest Interface:");
        ps.println("    getCharacterEncoding:  " + request.getCharacterEncoding());
        ps.println("    getContentType:        " + request.getContentType());
        ps.println("    getContentLength:      " + request.getContentLength());
        ps.println("    getProtocol:           " + request.getProtocol());
        ps.println("    getScheme:             " + request.getScheme());
        ps.println("    getServerName:         " + request.getServerName());
        ps.println("    getServerPort:         " + request.getServerPort());
        ps.println("    getRemoteAddr:         " + request.getRemoteAddr());
        ps.println("    getRemoteHost:         " + request.getRemoteHost());
        //ps.println("    getRealPath:           "+request.getRealPath());


        ps.println(".............................");
        ps.println("");
        i = 0;
        e = request.getAttributeNames();
        ps.println("    Attribute Names:");
        while(e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Attribute[" + i + "]: " + s);
            ps.println(" Type: " + request.getAttribute(s));
        }

        ps.println(".............................");
        ps.println("");
        i = 0;
        e = request.getParameterNames();
        ps.println("    Parameter Names:");
        while(e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Parameter[" + i + "]: " + s);
            ps.println(" Value: " + request.getParameter(s));
        }

        ps.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        ps.println(" . . . . . . . . . Servlet Infomation API  . . . . . . . . . . . . . .");
        ps.println("");

        ps.println("Servlet Context:");
        ps.println("");

        i = 0;
        e = servletContext.getAttributeNames();
        ps.println("    Attribute Names:");
        while(e.hasMoreElements()) {
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
        while(e.hasMoreElements()) {
            String p = (String) e.nextElement();
            ps.print("        InitParameter[" + i + "]: " + p);
            ps.println(" Value: " + scnfg.getInitParameter(p));
            i++;
        }
        ps.println("");
        ps.println("######################## END PROBE ###############################");
        ps.println("");

    }

    /**
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
    public String getServerName()
    {
        // Ascertain the name of this server.
        String servletName = this.getClass().getName();
        return (servletName);
    }

    /**
     * Handles incoming requests from clients. Parses the request and determines
     * what kind of OPeNDAP response the client is requesting. If the request is
     * understood, then the appropriate handler method is called, otherwise
     * an error is returned to the client.
     * <p/>
     * This method is the entry point for <code>DTSServlet</code>.
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

        log.debug("DTS doGet()");

        long tid = Thread.currentThread().getId();
        log.debug("thread=" + tid);

        // setHeader("Last-Modified", (new Date()).toString() );

        boolean isDebug = false;
        ReqState rs = null;
        RequestDebug reqD = null;
        try {
//      if(Debug.isSet("probeRequest"))
//        probeRequest(System.out, rs);

            rs = getRequestState(request, response);
            assert (rs != null);
            if(rs != null) {
                String ds = rs.getDataSet();
                String suff = rs.getRequestSuffix();
                isDebug = ((ds != null) && ds.equals("debug") && (suff != null) && suff.equals(""));
            }

            synchronized (syncLock) {

                if(!isDebug) {
                    long reqno = HitCounter++;
                    if(track) {
                        reqD = new RequestDebug(reqno, Thread.currentThread().toString());
                        rs.setUserObject(reqD);
                        if(prArr == null) prArr = new ArrayList(10000);
                        prArr.add((int) reqno, rs);
                    }

                    if(Debug.isSet("showRequest")) {
                        log.debug("-------------------------------------------");
                        log.debug("Server: " + getServerName() + "   Request #" + reqno);
                        log.debug("Client: " + request.getRemoteHost());
                        log.debug(rs.toString());
                        log.debug("Request dataset: '" + rs.getDataSet() + "' suffix: '" + rs.getRequestSuffix() +
                            "' CE: '" + rs.getConstraintExpression() + "'");
                    }
                }
            } // synch

            if(rs != null) {
                String dataSet = rs.getDataSet();
                String requestSuffix = rs.getRequestSuffix();
                rs.getResponse().setHeader("XDODS-Server", getServerVersion());// Make sure always set


                if(dataSet == null || dataSet.equals("/") || dataSet.equals("")) {
                    doGetDIR(rs);
                } else if(dataSet.equalsIgnoreCase("/version") || dataSet.equalsIgnoreCase("/version/")) {
                    doGetVER(rs);
                } else if(dataSet.equalsIgnoreCase("/help") || dataSet.equalsIgnoreCase("/help/")) {
                    doGetHELP(rs);
                } else if(dataSet.equalsIgnoreCase("/" + requestSuffix)) {
                    doGetHELP(rs);
                } else if(requestSuffix.equalsIgnoreCase("dds")) {
                    doGetDDS(rs);
                } else if(requestSuffix.equalsIgnoreCase("das")) {
                    doGetDAS(rs);
                } else if(requestSuffix.equalsIgnoreCase("ddx")) {
                    doGetDDX(rs);
                } else if(requestSuffix.equalsIgnoreCase("blob")) {
                    doGetBLOB(rs);
                } else if(requestSuffix.equalsIgnoreCase("dods")) {
                    doGetDAP2Data(rs);
                } else if(requestSuffix.equalsIgnoreCase("asc") ||
                    requestSuffix.equalsIgnoreCase("ascii")) {
                    doGetASC(rs);
                } else if(requestSuffix.equalsIgnoreCase("info")) {
                    doGetINFO(rs);
                } else if(requestSuffix.equalsIgnoreCase("html") || requestSuffix.equalsIgnoreCase("htm")) {
                    doGetHTML(rs);
                } else if(requestSuffix.equalsIgnoreCase("ver") || requestSuffix.equalsIgnoreCase("version")) {
                    doGetVER(rs);
                } else if(requestSuffix.equalsIgnoreCase("help")) {
                    doGetHELP(rs);

        /* JC added
        } else if(dataSet.equalsIgnoreCase("catalog") && requestSuffix.equalsIgnoreCase("xml")) {
          doGetCatalog(rs);
        } else if(dataSet.equalsIgnoreCase("status")) {
          doGetStatus(rs);
        } else if(dataSet.equalsIgnoreCase("systemproperties")) {
          doGetSystemProps(rs);
        } else if(isDebug) {
          doDebug(rs);  */
                } else if(requestSuffix.equals("")) {
                    badURL(request, response);
                } else {
                    badURL(request, response);
                }
            } else {// rs == null
                badURL(request, response);
            }

            if(reqD != null) reqD.done = true;
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

    void showMemUsed(String from)
    {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        //long maxMemory = Runtime.getRuntime ().maxMemory ();
        long usedMemory = (totalMemory - freeMemory);

        log.debug("****showMemUsed " + from);
        log.debug(" totalMemory " + totalMemory);
        log.debug(" freeMemory " + freeMemory);
        //log.debug(" maxMemory "+maxMemory);
        log.debug(" usedMemory " + usedMemory);
    }


    /**
     * Prints the OPeNDAP Server help page to the passed PrintWriter
     *
     * @param pw PrintWriter stream to which to dump the help page.
     */
    private void printHelpPage(PrintWriter pw)
    {
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

    /**
     * Prints the Bad URL Page page to the passed PrintWriter
     *
     * @param pw PrintWriter stream to which to dump the bad URL page.
     */
    private void printBadURLPage(PrintWriter pw)
    {
        pw.println("<h3>Error in URL</h3>");
        pw.println("The URL extension did not match any that are known by this");
        pw.println("server. Below is a list of the five extensions that are be recognized by");
        pw.println("all OPeNDAP servers. If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a><p>");

    }

    //////////////////////////////////////////////////
    // debug

    private ArrayList prArr = null;

    static private class RequestDebug
    {
        long reqno;
        String threadDesc;
        boolean done = false;

        RequestDebug(long reqno, String threadDesc)
        {
            this.reqno = reqno;
            this.threadDesc = threadDesc;
        }
    }
}





