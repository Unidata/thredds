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



package opendap.test.servers;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.zip.DeflaterOutputStream;


import opendap.servlet.AbstractServlet;

import opendap.dap.*;
import opendap.dap.parsers.ParseException;
import opendap.Server.*;
import opendap.servlet.GuardedDataset;
import opendap.servlet.ReqState;
import opendap.servlet.AsciiWriter;


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
 * <p/>
 *            &lt;servlet-class&gt;
 *                opendap.test.servers.dts
 *            &lt;/servlet-class&gt;
 * <p/>
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;DebugOn&lt;/param-name&gt;
 *                &lt;param-value&gt;showRequest showResponse &lt;/param-value&gt;
 *            &lt;/init-param&gt;
 * <p/>
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;INFOcache&lt;/param-name&gt;
 *                &lt;param-value&gt;/usr/Java-OPeNDAP/sdds-testsuite/info/&lt;/param-value&gt;
 *            &lt;/init-param&gt;
 * <p/>
 *            &lt;init-param&gt;
 *                &lt;param-name&gt;DDScache&lt;/param-name&gt;
 *                &lt;param-value&gt;/usr/Java-OPeNDAP/sdds-testsuite/dds/&lt;/param-value&gt;
 *            &lt;/init-param&gt;
 * <p/>
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


public class dts extends AbstractServlet {


    private static FunctionLibrary functionLibrary =
            new FunctionLibrary("opendap.test.servers.SSF");




    public void init() throws ServletException {

        super.init();
        System.out.println("*************************************************");
        System.out.println("*************************************************");
        System.out.println("**************** DTS INIT ***********************");
        System.out.println("*************************************************");
        System.out.println("*************************************************");

    }


    /**
     * ************************************************************************
     * OPeNDAP Server Version for the test server...
     *
     * @serial Ostensibly it's a serializable item...
     */

    private String ServerVersion = "DODS/3.2";


    /**
     * ************************************************************************
     * This method returns a String containing the OPeNDAP Server Version...
     */
    public String getServerVersion() {
        return (ServerVersion);
    }

    /**
     * ************************************************************************
     * We override this crucial method from the parent servlet in order to
     * force the client not to cache. This is achieved by setting the header
     * tag "Last-Modified" to the current date and time.
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {


        response.setHeader("Last-Modified", (new Date()).toString());
        System.out.println("*************************************************");
        System.out.println("**************** DTS doGet() ********************");
        System.out.println("*************************************************");


        super.doGet(request, response);


    }

    /**
     * ************************************************************************
     * Default handler for the client's data request. Requires the getDDS()
     * method implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed, the data is read (using the class in the
     * localized server factory etc.), compared to the constraint expression,
     * and then sent to the client.
     *
     * @param request              The client's <code> HttpServletRequest</code> request
     *                             object.
     * @param response             The server's <code> HttpServletResponse</code> response
     *                             object.
     */
    public void doGetDAP2Data(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws IOException, ServletException {


        System.out.println("*************************************************");
        System.out.println("*************************************************");
        System.out.println("**************** DTS INIT ***********************");
        System.out.println("*************************************************");
        System.out.println("*************************************************");

        System.out.println("Sending OPeNDAP Data For: " + rs.getDataSet());

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", getServerVersion());
        response.setHeader("Content-Description", "dods_data");
        response.setStatus(HttpServletResponse.SC_OK);

        ServletOutputStream sOut = response.getOutputStream();
        OutputStream bOut, eOut;


        if (rs.getAcceptsCompressed()) {
            response.setHeader("Content-Encoding", "deflate");
            bOut = new DeflaterOutputStream(sOut);
        } else {
            // Commented out because of a bug in the OPeNDAP C++ stuff...
            //response.setHeader("Content-Encoding", "plain");
            bOut = new BufferedOutputStream(sOut);
        }


        try {

            GuardedDataset ds = getDataset(rs);
            // Utilize the getDDS() method to get a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = (ServerDDS) ds.getDDS();

            cacheArrayShapes(myDDS);

            //myDDS.print(System.out);

            // Instantiate the CEEvaluator
            CEEvaluator ce =
                    new CEEvaluator(myDDS,
                            new ClauseFactory(functionLibrary));

            // and parse the constraint expression
            ce.parseConstraint(rs.getConstraintExpression());

            // Send the constrained DDS back to the client
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(bOut));
            myDDS.printConstrained(pw);
            //myDDS.printConstrained(System.out);

            // Send the Data delimiter back to the client
            pw.flush();
            bOut.write("\nData:\n".getBytes()); // JCARON CHANGED
            bOut.flush();

            // Send the binary data back to the client
            DataOutputStream sink = new DataOutputStream(bOut);


            int seqLength = 5;

            String sls = rs.getInitParameter("SequenceLength");
            if (sls != null) {
                seqLength = Integer.valueOf(sls);
            }

            testEngine te = new testEngine(seqLength);
            ce.send(myDDS.getName(), sink, te);
            sink.flush();

            // Finish up sending the compressed stuff, but don't
            // close the stream (who knows what the Servlet may expect!)
            if (rs.getAcceptsCompressed())
                ((DeflaterOutputStream) bOut).finish();

        }
        catch (DAP2Exception de) {
            dap2ExceptionHandler(de, response);
        }
        catch (ParseException pe) {
            parseExceptionHandler(pe, response);
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
     * @param request              The client's <code> HttpServletRequest</code> request
     *                             object.
     * @param response             The server's <code> HttpServletResponse</code> response
     *                             object.
     */
    public void doGetASC(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws Exception  {


        System.out.println("*************************************************");
        System.out.println("Sending OPeNDAP ASCII Response  For: " + rs.getDataSet());

        response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/plain");
        response.setHeader("Content-Description", "dods_ascii");
        response.setStatus(HttpServletResponse.SC_OK);





        try {

            GuardedDataset ds = getDataset(rs);
            // Utilize the getDDS() method to get a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = ds.getDDS();

            cacheArrayShapes(myDDS);

            //myDDS.print(System.out);

            // Instantiate the CEEvaluator
            CEEvaluator ce =
                    new CEEvaluator(myDDS,
                            new ClauseFactory(functionLibrary));

            // and parse the constraint expression
            ce.parseConstraint(rs.getConstraintExpression());

            int seqLength = 5;

            String sls = rs.getInitParameter("SequenceLength");
            if (sls != null) {
                seqLength = Integer.valueOf(sls);
            }

            System.out.println("Sequence Length: "+seqLength);

            testEngine te = new testEngine(seqLength);



            PrintWriter pw = new PrintWriter(response.getOutputStream());
            myDDS.printConstrained(pw);
            pw.println("---------------------------------------------");


            AsciiWriter writer = new AsciiWriter(); // could be static


            writer.toASCII(pw,myDDS,te);



            pw.flush();

            System.out.println("ASCII Response Sent");


        }
        catch (DAP2Exception de) {
            dap2ExceptionHandler(de, response);
        }
        catch (ParseException pe) {
            parseExceptionHandler(pe, response);
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
     * @param request              The client's <code> HttpServletRequest</code> request
     *                             object.
     * @param response             The server's <code> HttpServletResponse</code> response
     *                             object.
     *
     * @opendap.ddx.experimental
     */
    public void doGetBLOB(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws IOException, ServletException {


        System.out.println("Sending BLOB Data For: " + rs.getDataSet());

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", getServerVersion());
        response.setHeader("Content-Description", "dods_blob");


        ServletOutputStream sOut = response.getOutputStream();
        OutputStream bOut, eOut;


        if (rs.getAcceptsCompressed()) {
            response.setHeader("Content-Encoding", "deflate");
            bOut = new DeflaterOutputStream(sOut);
        } else {
            // Commented out because of a bug in the OPeNDAP C++ stuff...
            //response.setHeader("Content-Encoding", "plain");
            bOut = new BufferedOutputStream(sOut);
        }


        try {

            GuardedDataset ds = getDataset(rs);
            // Utilize the getDDS() method to get a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = (ServerDDS) ds.getDDS();

            cacheArrayShapes(myDDS);

            // Instantiate the CEEvaluator
            CEEvaluator ce =
                    new CEEvaluator(myDDS,
                            new ClauseFactory(functionLibrary));

            // and parse the constraint expression
            ce.parseConstraint(rs.getConstraintExpression());

            // Send the binary data back to the client
            DataOutputStream sink = new DataOutputStream(bOut);


            int seqLength = 5;

            String sls = rs.getInitParameter("SequenceLength");
            if (sls != null) {
                seqLength = (Integer.valueOf(sls)).intValue();
            }

            testEngine te = new testEngine(seqLength);
            ce.send(myDDS.getName(), sink, te);
            sink.flush();

            // Finish up sending the compressed stuff, but don't
            // close the stream (who knows what the Servlet may expect!)
            if (rs.getAcceptsCompressed())
                ((DeflaterOutputStream) bOut).finish();

        }
        catch (DAP2Exception de) {
            dap2ExceptionHandler(de, response);
        }
        catch (ParseException pe) {
            parseExceptionHandler(pe, response);
        }


        response.setStatus(response.SC_OK);


    }

    /**
     * ***********************************************************************
     */


    private void cacheArrayShapes(ServerDDS sdds) {

        Enumeration e = sdds.getVariables();

        while (e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();
            cAS(bt);
        }
    }


    private void cAS(BaseType bt) {

        if (bt instanceof DConstructor) {
            Enumeration e = ((DConstructor) bt).getVariables();
            while (e.hasMoreElements()) {
                BaseType tbt = (BaseType) e.nextElement();
                cAS(tbt);
            }
        } else if (bt instanceof test_SDArray) {
            ((test_SDArray) bt).cacheShape();
        }


    }

    protected GuardedDataset getDataset(ReqState rs) throws DAP2Exception, IOException, ParseException {
        return new testDataset(rs);
    }


}





