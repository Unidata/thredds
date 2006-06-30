/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//
/////////////////////////////////////////////////////////////////////////////

/* $Id: dodsDIR.java,v 1.1 2005/12/16 22:37:05 caron Exp $
*
*/

package dods.servlet;

import java.io.*;
//import java.text.*;
//import java.net.MalformedURLException;
//import java.util.*;
//import java.util.zip.DeflaterOutputStream;
//import javax.servlet.*;
import javax.servlet.http.*;

import dods.dap.*;
//import dods.util.*;
//import dods.servers.ascii.*;
import dods.dap.parser.ParseException;

/**
* Default handler for DODS directory requests. This class is used
* by DODSServlet. This code exists as a seperate class in order to alleviate
* code bloat in the DODSServlet class. As such, it contains virtually no
* state, just behaviors.
*
* @author Nathan David Potter
*/

public class dodsDIR {

    private static final boolean _Debug = false;


    /***************************************************************************
    * Default handler for DODS directory requests. Returns an html document
    * with a list of all datasets on this server with links to their
    * DDS, DAS, Information, and HTML responses.
    *
    * @param request The <code>HttpServletRequest</code> from the client.
    *
    * @param response The <code>HttpServletResponse</code> for the client.
    *
    * @param rs The ReqState object for this client request.
    */
    public void sendDIR(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
                          throws DODSException, ParseException {

        if (_Debug) System.out.println("sendDIR request = "+request);

        String ddsCacheDir = rs.getInitParameter("DDScache");

        if(ddsCacheDir == null)
            ddsCacheDir = rs.getDDSCache();


        File ddsDir = new File(ddsCacheDir);
        String separator = "/";
        try {

            PrintWriter pw = new PrintWriter(response.getOutputStream());

            if(ddsDir.exists()){

                if(ddsDir.isDirectory()){

                    String thisServer = request.getRequestURL().toString();
                    if(_Debug) System.out.println("lastIndexOf("+separator+"): " + thisServer.lastIndexOf(separator));
                    if(_Debug) System.out.println("length: " + thisServer.length());

                    if(thisServer.lastIndexOf(separator) != (thisServer.length()-1) ){
                        if(_Debug) System.out.println("Server URL does not end with: " + separator);
                        thisServer += separator;
                    }
                    else {
                        if(_Debug) System.out.println("Server URL ends with: " + separator);
                    }


                    pw.println("<html>");
                    pw.println("<head>");
                    pw.println("<title>DODS Directory</title>");
                    pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
                    pw.println("</head>");

                    pw.println("<body bgcolor=\"#FFFFFF\">");


                    pw.println("<h1>DODS Directory for:</h1>");
                    pw.println("<h2>" + thisServer+"</h2>");
                    pw.println("<hr>");


                    File fList[] = ddsDir.listFiles();

                    pw.println("<table border=\"0\">");

                    for(int i=0; i<fList.length ;i++){
                        if(fList[i].isFile()){

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
                    pw.println("<hr>");
                    pw.println("</html>");
                }
                else {
                    pw.println("<hr>");
                    pw.println("<h1>");
                    pw.println("Specified DDS cache:</h1>");
                    pw.println("<h2><i>"+ddsCacheDir+"</i></h2>");
                    pw.println("not a directory!");
                    pw.println("</h1>");
                    pw.println("<hr>");
                }
            }
            else{
                pw.println("<hr>");
                pw.println("<h1>Cannot Find DDS Directory:<br></h1>");
                pw.println("<h2><i>"+ddsCacheDir+"</i></h2>");
                pw.println("<hr>");
            }

            pw.flush();

        }
        catch (FileNotFoundException fnfe){
            System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
            fnfe.printStackTrace(System.out);
        }
        catch (IOException ioe){
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }


    }

}



