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

import opendap.dap.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Default handler for OPeNDAP directory requests. This class is used
 * by AbstractServlet. This code exists as a seperate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetDirHandler {

    private static final boolean _Debug = false;
    private String separator = "/";


    /**
     * ************************************************************************
     * Default handler for OPeNDAP directory requests. Returns an html document
     * with a list of all datasets on this server with links to their
     * DDS, DAS, Information, and HTML responses.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @param rs       The request state object for this client request.
     * @see ReqState
     */
    public void sendDIR(HttpServletRequest request,
                        HttpServletResponse response,
                        ReqState rs)
            throws opendap.dap.DAP2Exception, ParseException {

        if (_Debug) System.out.println("sendDIR request = " + request);

        String ddxCacheDir = rs.getDDXCache();
        String ddsCacheDir = rs.getDDSCache();

        try {

            PrintWriter pw = new PrintWriter(response.getOutputStream());

            String thisServer = request.getRequestURL().toString();
            pw.println("<html>");
            pw.println("<head>");
            pw.println("<title>OPeNDAP Directory</title>");
            pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
            pw.println("</head>");

            pw.println("<body bgcolor=\"#FFFFFF\">");


            pw.println("<h1>OPeNDAP Directory for:</h1>");
            pw.println("<h2>" + thisServer + "</h2>");

            printDIR(pw, ddxCacheDir, "DDX", thisServer);

            printDIR(pw, ddsCacheDir, "DDS", thisServer);
            pw.println("<hr>");
            pw.println("</html>");
            pw.flush();

        } catch (FileNotFoundException fnfe) {
            System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
            fnfe.printStackTrace(System.out);
        } catch (IOException ioe) {
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }


    }


    private void printDIR(PrintWriter pw, String dirName, String dirType, String thisServer) {

        pw.println("<hr>");
        pw.println("<h3>" + dirType + "</h3>");

        File dir = new File(dirName);

        if (dir.exists()) {

            if (dir.isDirectory()) {

                if (_Debug) System.out.println("lastIndexOf(" + separator + "): " + thisServer.lastIndexOf(separator));
                if (_Debug) System.out.println("length: " + thisServer.length());

                if (thisServer.lastIndexOf(separator) != (thisServer.length() - 1)) {
                    if (_Debug) System.out.println("Server URL does not end with: " + separator);
                    thisServer += separator;
                } else {
                    if (_Debug) System.out.println("Server URL ends with: " + separator);
                }


                File fList[] = dir.listFiles();

                pw.println("<table border=\"0\">");

                for (int i = 0; i < fList.length; i++) {
                    if (fList[i].isFile()) {

                        pw.println("<tr>");

                        pw.print("    <td>");
                        pw.print("<div align='right'>");
                        pw.print("<b>" +
                                fList[i].getName() +
                                ":</b> ");
                        pw.print("</div>");
                        pw.println("</td>");


                        pw.print("    <td>");
                        pw.print("<div align='center'>");
                        pw.print("<a href='" +
                                thisServer +
                                fList[i].getName() +
                                ".ddx'> DDX </a>");
                        pw.print("</div>");
                        pw.println("</td>");

                        pw.print("    <td>");
                        pw.print("<div align='center'>");
                        pw.print("<a href='" +
                                thisServer +
                                fList[i].getName() +
                                ".dds'> DDS </a>");
                        pw.print("</div>");
                        pw.println("</td>");

                        pw.print("    <td>");
                        pw.print("<div align='center'>");
                        pw.print("<a href='" +
                                thisServer +
                                fList[i].getName() +
                                ".das'> DAS </a>");
                        pw.print("</div>");
                        pw.println("</td>");

                        pw.print("    <td>");
                        pw.print("<div align='center'>");
                        pw.print("<a href='" +
                                thisServer +
                                fList[i].getName() +
                                ".info'> Information </a>");
                        pw.print("</div>");
                        pw.println("</td>");

                        pw.print("    <td>");
                        pw.print("<div align='center'>");
                        pw.print("<a href='" +
                                thisServer +
                                fList[i].getName() +
                                ".html'> HTML Data Request Form </a>");
                        pw.print("</div>");
                        pw.println("</td>");

                        pw.println("</tr>");
                    }
                }
                pw.println("</table>");

            } else {
                pw.println("<h3>");
                pw.println("Specified " + dirType + " cache:<br>");
                pw.println("<i>" + dirName + "</i><br>");
                pw.println("is not a directory!");
                pw.println("</h3>");
            }
        } else {
            pw.println("<h4>");
            pw.println("Cannot Find " + dirType + " Directory:<br>");
            pw.println("<i>" + dirName + "</i><br>");
            pw.println("</h4>");
        }


    }


}





