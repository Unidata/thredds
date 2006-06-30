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

/* $Id: dodsHTML.java,v 1.1 2005/12/16 22:37:05 caron Exp $
*
*/

package dods.servlet;



import java.io.*;
import javax.servlet.http.*;

import dods.servers.www.*;
import dods.dap.*;
import dods.dap.parser.ParseException;
import dods.dap.Server.ServerDDS;

/**
* Default handler for DODS .html requests. This class is used
* by DODSServlet. This code exists as a seperate class in order to alleviate
* code bloat in the DODSServlet class. As such, it contains virtually no
* state, just behaviors.
*
* @author Nathan David Potter
*/

public class dodsHTML {

    private static final boolean debug = false;
    private String helpLocation = "http://unidata.ucar.edu/packages/dods/help_files/";




    /***************************************************************************
    * Default handler for DODS .html requests. Returns an html form
    * and javascript code that allows the user to use their browser
    * to select variables and build constraints for a data request.
    * The DDS and DAS for the data set are used to build the form. The
    * types in dods.servers.www are integral to the form generation.
    *
    * @param request The <code>HttpServletRequest</code> from the client.
    *
    * @param response The <code>HttpServletResponse</code> for the client.
    *
    * @param dataSet The Name of the data set.
     *
    *
    * @see dods.servers.www
    */
    public void sendDataRequestForm(HttpServletRequest request,
                          HttpServletResponse response,
			  String dataSet,
                          ServerDDS sdds,
                          DAS myDAS) // changed jc
                          throws DODSException, ParseException {


        if (debug) System.out.println("Sending DODS Data Request Form For: " + dataSet +
			     "    CE: '" + request.getQueryString()+ "'");
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


	int suffixIndex = request.getRequestURL().toString().lastIndexOf(".");

	requestURL = request.getRequestURL().substring(0,suffixIndex);


        try{



	    //PrintWriter pw = new PrintWriter(response.getOutputStream());
	    PrintWriter pw ;
	    if(false){
	        pw = new PrintWriter(
	                         new FileOutputStream(
			         new File("debug.html")
			         )
			         );
            }
	    else
	        pw = new PrintWriter(response.getOutputStream());


	    wwwOutPut wOut = new wwwOutPut(pw);

            // Get the DDS and the DAS (if one exists) for the dataSet.
            DDS myDDS = getWebFormDDS(dataSet, sdds);
            //DAS myDAS = dServ.getDAS(dataSet); // change jc

	    jscriptCore jsc = new jscriptCore();

            pw.println(
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"\n"
                + "\"http://www.w3.org/TR/REC-html40/loose.dtd\">\n"
                + "<html><head><title>DODS Dataset Query Form</title>\n"
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
                + "<p><h2 align='center'>DODS Dataset Access Form</h2>\n"
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
	catch (IOException ioe){
	    System.out.println("OUCH! IOException: " + ioe.getMessage());
	    ioe.printStackTrace(System.out);
	}


    }




    /***************************************************************************
    * Gets a DDS for the specified data set and builds it using the class
    * factory in the package <b>dods.servers.www</b>.
    * <p>
    * Currently this method uses a deprecated API to perform a translation
    * of DDS types. This is a known problem, and as soon as an alternate
    * way of achieving this result is identified we will implement it.
    * (Your comments appreciated!)
    *
    * @param dataSet A <code>String</code> containing the data set name.
    *
    * @param sDDS The <code>ServerDDS</code> reference for the servlet
    * pbject that's running this show.
    *
    * @return A DDS object built using the www interface class factory.
    *
    * @see dods.dap.DDS
    * @see dods.servers.www
    * @see dods.servers.www.wwwFactory
    */
    public DDS getWebFormDDS(String dataSet, ServerDDS sDDS) // changed jc
                                            throws DODSException, ParseException{


	// Get the DDS we need, using the getDDS method
	// for this particular server
	// ServerDDS sDDS = dServ.getDDS(dataSet);

	// Make a new DDS using the web form (www interface) class factory
	wwwFactory wfactory = new wwwFactory();
	DDS wwwDDS = new DDS(dataSet,wfactory);

	// Make a special print writer to catch the ServerDDS's
	// persistent representation in a String.
	StringWriter ddsSW = new StringWriter();
	sDDS.print(new PrintWriter(ddsSW));

	// Now use that string to make an input stream to
	// pass to our new DDS for parsing.
	wwwDDS.parse( new StringBufferInputStream(ddsSW.toString()));


	return(wwwDDS);


    }



}


