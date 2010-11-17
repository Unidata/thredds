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
import java.net.MalformedURLException;
import java.util.*;
import javax.servlet.http.*;

import opendap.dap.*;
import opendap.util.*;
import opendap.servers.ascii.*;
import opendap.dap.parser.ParseException;

/**
 * Default handler for OPeNDAP ascii requests. This class is used
 * by AbstractServlet. This code exists as a seperate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetAsciiHandler {

    private static final boolean _Debug = false;


    /**
     * ************************************************************************
     * Default handler for OPeNDAP ascii requests. Returns OPeNDAP DAP2 data in
     * comma delimited ascii columns for ingestion into some not so
     * OPeNDAP enabled application such as MS-Excel. Accepts constraint
     * expressions in exactly the same way as the regular OPeNDAP dataserver.
     *
     * @param request
     * @param response
     * @param dataSet
     * @throws opendap.dap.DAP2Exception
     * @throws ParseException
     */
    public void sendASCII(HttpServletRequest request,
                          HttpServletResponse response,
                          String dataSet)
            throws DAP2Exception, ParseException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending OPeNDAP ASCII Data For: " + dataSet +
                    "    CE: '" + request.getQueryString() + "'");


        String requestURL, ce;
        DConnect url;
        DataDDS dds;

        if (request.getQueryString() == null) {
            ce = "";
        } else {
            ce = "?" + request.getQueryString();
        }

        int suffixIndex = request.getRequestURL().toString().lastIndexOf(".");

        requestURL = request.getRequestURL().substring(0, suffixIndex);

        if (Debug.isSet("showResponse")) {
            System.out.println("New Request URL Resource: '" + requestURL + "'");
            System.out.println("New Request Constraint Expression: '" + ce + "'");
        }

        try {

            if (_Debug) System.out.println("Making connection to .dods service...");
            url = new DConnect(requestURL, true);

            if (_Debug) System.out.println("Requesting data...");
            dds = url.getData(ce, null, new asciiFactory());

            if (_Debug) System.out.println(" ASC DDS: ");
            if (_Debug) dds.print(System.out);

            PrintWriter pw = new PrintWriter(response.getOutputStream());
            PrintWriter pwDebug = new PrintWriter(System.out);


            if(dds!=null){
                dds.print(pw);
                pw.println("---------------------------------------------");


                String s = "";
                Enumeration e = dds.getVariables();

                while (e.hasMoreElements()) {
                    BaseType bt = (BaseType) e.nextElement();
                    if (_Debug) ((toASCII) bt).toASCII(pwDebug, true, null, true);
                    //bt.toASCII(pw,addName,getNAme(),true);
                    ((toASCII) bt).toASCII(pw, true, null, true);
                }
            }
            else {

                String betterURL = request.getRequestURL().substring(0, request.getRequestURL().lastIndexOf(".")) +
                        ".dods?"+ request.getQueryString();



                pw.println("-- ASCII RESPONSE HANDLER PROBLEM --");
                pw.println("");
                pw.println("The ASCII response handler was unable to obtain requested data set.");
                pw.println("");
                pw.println("Because this handler calls it's own OPeNDAP server to get the requested");
                pw.println("data the source error is obscured.");
                pw.println("");
                pw.println("To get a better idea of what is going wrong, try requesting the URL:");
                pw.println("");
                pw.println("    "+betterURL);
                pw.println("");
                pw.println("And then look carefully at the returned document. Note that if you");
                pw.println("are using a browser to access the URL the returned document will");
                pw.println("more than likely be treated as a download and written to your");
                pw.println("local disk. It should be a file with the extension \".dods\"");
                pw.println("");
                pw.println("Locate it, open it with a text editor, and find your");
                pw.println("way to happiness and inner peace.");
                pw.println("");
            }

            //pw.println("</pre>");
            pw.flush();
            if (_Debug) pwDebug.flush();

        }
        catch (FileNotFoundException fnfe) {
            System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
            fnfe.printStackTrace(System.out);
        }
        catch (MalformedURLException mue) {
            System.out.println("OUCH! MalformedURLException: " + mue.getMessage());
            mue.printStackTrace(System.out);
        }
        catch (IOException ioe) {
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }
        catch (Throwable t) {
            System.out.println("OUCH! Throwable: " + t.getMessage());
            t.printStackTrace(System.out);
        }

        if (_Debug) System.out.println(" GetAsciiHandler done");
    }
    /***************************************************************************/


}





