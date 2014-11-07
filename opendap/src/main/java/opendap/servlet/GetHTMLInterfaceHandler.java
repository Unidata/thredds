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
import javax.servlet.http.*;

import opendap.dap.*;
import opendap.dap.parsers.*;
import opendap.servers.ServerDDS;
import opendap.servlet.www.jscriptCore;
import opendap.servlet.www.wwwFactory;
import opendap.servlet.www.wwwOutPut;

/**
 * Default handler for OPeNDAP .html requests. This class is used
 * by AbstractServlet. This code exists as a seperate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetHTMLInterfaceHandler {

    private static final boolean _Debug = false;
    private String helpLocation = "http://www.opendap.org/online_help_files/";

    /**
     * ************************************************************************
     * Default handler for OPeNDAP .html requests. Returns an html form
     * and javascript code that allows the user to use their browser
     * to select variables and build constraints for a data request.
     * The DDS and DAS for the data set are used to build the form. The
     * types in opendap.servlet.www are integral to the form generation.
     *
     * @param rs  The <code>ReqState</code> from the client.
     * @param dataSet
     * @param sdds
     * @param myDAS
     * @throws opendap.dap.DAP2Exception
     * @throws ParseException
     * @see opendap.servlet.www.wwwFactory
     */
    public void sendDataRequestForm(ReqState rs,
                                    String dataSet,
                                    ServerDDS sdds,
                                    DAS myDAS) // changed jc
            throws DAP2Exception, ParseException {


        if (_Debug) System.out.println("Sending DODS Data Request Form For: " + dataSet +
                "    CE: '" + rs.getRequest().getQueryString() + "'");
        String requestURL;

/*
        // Turn this on later if we discover we're supposed to accept
        // constraint expressions as input to the Data Request Web Form
    String ce;
    if(request.getQueryString() == null){
        ce = "";
        }
    else {
        ce = "?" + request.getQueryString();
        }
*/


        int suffixIndex = rs.getRequest().getRequestURL().toString().lastIndexOf(".");

        requestURL = rs.getRequest().getRequestURL().substring(0, suffixIndex);

        String dapCssUrl = "/" + requestURL.split("/",5)[3] + "/" + "tdsDap.css";
        

        try {

            //PrintWriter pw = new PrintWriter(response.getOutputStream());
            PrintWriter pw;
            if (false) {
                pw = new PrintWriter(
                        new FileOutputStream(
                                new File("debug.html")
                        )
                );
            } else
                pw = new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(),Util.UTF8));


            wwwOutPut wOut = new wwwOutPut(pw);

            // Get the DDS and the DAS (if one exists) for the dataSet.
            DDS myDDS = getWebFormDDS(dataSet, sdds);
            //DAS myDAS = dServ.getDAS(dataSet); // change jc

            jscriptCore jsc = new jscriptCore();

            pw.println(
                    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"\n"
                            + "\"http://www.w3.org/TR/REC-html40/loose.dtd\">\n"
                            + "<html><head><title>OPeNDAP Dataset Query Form</title>\n"
                            + "<link type=\"text/css\" rel=\"stylesheet\" media=\"screen\" href=\"" + dapCssUrl + "\"/>\n"
                            + "<base href=\"" + helpLocation + "\">\n"
                            + "<script type=\"text/javascript\">\n"
                            + "<!--\n"
            );
            pw.flush();

            pw.println(jsc.jScriptCode);
            pw.flush();

            pw.println(
                    "DODS_URL = new dods_url(\"" + requestURL + "\");\n"
                            + "// -->\n"
                            + "</script>\n"
                            + "</head>\n"
                            + "<body>\n"
                            + "<p><h2 align='center'>OPeNDAP Dataset Access Form</h2>\n"
                            + "<hr>\n"
                            + "<font size=-1>Tested on Netscape 4.61 and Internet Explorer 5.00.</font>\n"
                            + "<hr>\n"
                            + "<form action=\"\">\n"
                            + "<table>\n"
            );
            pw.flush();

            wOut.writeDisposition(requestURL);
            pw.println("<tr><td><td><hr>\n");

            wOut.writeGlobalAttributes(myDAS, myDDS);
            pw.println("<tr><td><td><hr>\n");

            wOut.writeVariableEntries(myDAS, myDDS);
            pw.println("</table></form>\n");
            pw.println("<hr>\n");


            pw.println(
                    "<address>Send questions or comments to: "
                            + "<a href=\"mailto:support@unidata.ucar.edu\">"
                            + "support@unidata.ucar.edu"
                            + "</a></address>"
                            + "</body></html>\n"
            );

            pw.println("<hr>");
            pw.println("<h2>DDS:</h2>");

            pw.println("<pre>");
            myDDS.print(pw);
            pw.println("</pre>");
            pw.println("<hr>");
            pw.flush();


        }
        catch (IOException ioe) {
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }


    }


    /**
     * ************************************************************************
     * Gets a DDS for the specified data set and builds it using the class
     * factory in the package <b>opendap.servlet.www</b>.
     * <p/>
     * Currently this method uses a deprecated API to perform a translation
     * of DDS types. This is a known problem, and as soon as an alternate
     * way of achieving this result is identified we will implement it.
     * (Your comments appreciated!)
     *
     * @param dataSet A <code>String</code> containing the data set name.
3     * @return A DDS object built using the www interface class factory.
     * @see opendap.dap.DDS
     * @see opendap.servlet.www.wwwFactory
     */
    public DDS getWebFormDDS(String dataSet, ServerDDS sDDS) // changed jc
            throws DAP2Exception, ParseException {

        // Get the DDS we need, using the getDDS method
        // for this particular server
        // ServerDDS sDDS = dServ.getDDS(dataSet);

        // Make a special print writer to catch the ServerDDS's
        // persistent representation in a String.
        StringWriter ddsSW = new StringWriter();
        sDDS.print(new PrintWriter(ddsSW));

        // Now use that string to make an input stream to
        // pass to our new DDS for parsing.

	// Since parser expects/requires InputStream,
	// we must adapt utf16 string to at least utf-8

	ByteArrayInputStream bai = null;
        try {
	    bai = new ByteArrayInputStream(ddsSW.toString().getBytes("UTF-8"));
	} catch (UnsupportedEncodingException uee) {
	    throw new DAP2Exception("UTF-8 encoding not supported");
	}

        // Make a new DDS parser using the web form (www interface) class factory
        wwwFactory wfactory = new wwwFactory();
        DDS wwwDDS = new DDS(dataSet,wfactory);
        wwwDDS.setURL(dataSet);
        wwwDDS.parse(bai);
        return (wwwDDS);


    }


}




