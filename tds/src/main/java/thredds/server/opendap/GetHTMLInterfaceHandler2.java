/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package thredds.server.opendap;


import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DDS;
import opendap.servers.ServerDDS;
import opendap.dap.parsers.ParseException;
import opendap.servlet.www.jscriptCore;
import opendap.servlet.www.wwwFactory;
import opendap.servlet.www.wwwOutPut;
import thredds.server.config.ThreddsConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Default handler for OPeNDAP .html requests. This class is used
 * by AbstractServlet. This code exists as a seperate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetHTMLInterfaceHandler2
{

    private static final boolean _Debug = false;
    private String helpLocation = "http://www.opendap.org/online_help_files/";

    private String serverContactName = ThreddsConfig.get( "serverInformation.contact.name", "UNKNOWN" );
    private String serverContactEmail = ThreddsConfig.get( "serverInformation.contact.email", "UNKNOWN" );
    private String odapSupportEmail = "support@opendap.org";

  /**
     * ************************************************************************
     * Default handler for OPeNDAP .html requests. Returns an html form
     * and javascript code that allows the user to use their browser
     * to select variables and build constraints for a data request.
     * The DDS and DAS for the data set are used to build the form. The
     * types in opendap.servlet.www are integral to the form generation.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @param dataSet
     * @param sdds
     * @param myDAS
     * @throws opendap.dap.DAP2Exception
     * @throws opendap.dap.parsers.ParseException
     * @see opendap.servlet.www.wwwFactory
     */
    public void sendDataRequestForm(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String dataSet,
                                    ServerDDS sdds,
                                    DAS myDAS) // changed jc
            throws DAP2Exception, ParseException {


        if (_Debug) System.out.println("Sending DODS Data Request Form For: " + dataSet +
                "    CE: '" + request.getQueryString() + "'");
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

        requestURL = request.getRequestURL().substring(0, suffixIndex);

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
                pw = response.getWriter();


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

            pw.println( "<address>");
            pw.println( "<p>For questions or comments about this dataset, contact the administrator of this server ["
                      + serverContactName + "] at: <a href='mailto:" + serverContactEmail + "'>"
                      + serverContactEmail + "</a></p>");
            pw.println( "<p>For questions or comments about OPeNDAP, email OPeNDAP support at:"
                        + " <a href='mailto:" + odapSupportEmail + "'>" + odapSupportEmail + "</a></p>" );
          pw.println( "</address></body></html>" );

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

        // Make a new DDS using the web form (www interface) class factory
        wwwFactory wfactory = new wwwFactory();
        DDS wwwDDS = new DDS(dataSet, wfactory);
        wwwDDS.setURL(dataSet);

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
        wwwDDS.parse(bai);

        return (wwwDDS);


    }


}




