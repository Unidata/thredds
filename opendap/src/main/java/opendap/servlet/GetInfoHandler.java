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

import opendap.dap.*;
import opendap.util.*;
import opendap.dap.Server.*;
import opendap.dap.parser.ParseException;

/**
 * Default handler for OPeNDAP info requests. This class is used
 * by AbstractServlet. This code exists as a seperate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetInfoHandler {

    private static final boolean _Debug = false;

    private String infoDir = null;

    /**
     * ************************************************************************
     * Default handler for OPeNDAP info requests. Returns an html document
     * describing the contents of the servers datasets.
     * <p/>
     * The "INFOcache" &lt;init-param&gt; element in the web.xml file
     * specifies the designated location for:
     * <ul>
     * <li>".info" response override files.</li>
     * <li>Server specific HTML* files.</li>
     * <li>Dataset specific HTML* files .</li>
     * </ul>
     * <p/>
     * The server specific HTML* files must be named #servlet#.html
     * where #servlet# is the name of the servlet that is running as
     * the OPeNDAP server in question. This name is determined at run time
     * by using the class called Class ( this.getClass().getName() ).
     * <p/>
     * <p>In the C++ code the analogy is the per-cgi file names.</p>
     * <p/>
     * <p/>
     * The dataset specific HTML* files are located by catenating `.html'
     * to #name#, where #name# is the name of the dataset. If the filename part
     * of #name# is of the form [A-Za-z]+[0-9]*.* then this function also looks
     * for a file whose name is [A-Za-z].html For example, if #name# is
     * .../data/fnoc1.nc this function first looks for .../data/fnoc1.nc.html.
     * However, if that does not exist it will look for .../data/fnoc.html. This
     * allows one `per-dataset' file to be used for a collection of files with
     * the same root name.
     * </p>
     * <p/>
     * NB: An HTML* file contains HTML without the <html>, <head> or <body> tags
     * (my own notation).
     * <p/>
     * <h3>Look for the user supplied Server- and dataset-specific HTML* documents.</h3>
     *
     * @param pw  The PrintStream to which the output should be written.
     * @param gds The thread safe dataset.
     * @param rs  The ReqState object for theis client request.
     * @see GuardedDataset
     * @see ReqState
     */
    public void sendINFO(PrintStream pw, GuardedDataset gds, ReqState rs) throws DAP2Exception, ParseException {

        if (_Debug) System.out.println("opendap.servlet.GetInfoHandler.sendINFO() reached.");

        String responseDoc = null;
        ServerDDS myDDS = null;
        DAS myDAS = null;


        myDDS = gds.getDDS();
        myDAS = gds.getDAS();


        infoDir = rs.getINFOCache();


        responseDoc = loadOverrideDoc(infoDir, rs.getDataSet());

        if (responseDoc != null) {
            if (_Debug) System.out.println("override document: " + responseDoc);
            pw.print(responseDoc);
        } else {


            String user_html = get_user_supplied_docs(rs.getServerClassName(), rs.getDataSet());

            String global_attrs = buildGlobalAttributes(myDAS, myDDS);

            String variable_sum = buildVariableSummaries(myDAS, myDDS);

            // Send the document back to the client.
            pw.println("<html><head><title>Dataset Information</title>");
            pw.println("<style type=\"text/css\">");
            pw.println("<!-- ul {list-style-type: none;} -->");
            pw.println("</style>");
            pw.println("</head>");
            pw.println("<body>");

            if (global_attrs.length() > 0) {
                pw.println(global_attrs);
                pw.println("<hr>");
            }

            pw.println(variable_sum);

            pw.println("<hr>");

            pw.println(user_html);

            pw.println("</body></html>");

            // Flush the output buffer.
            pw.flush();
        }

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Checks the info directory for user supplied override documents for the
     * passed dataset name. If there are overridedocuments present then the
     * contents are read and returned to the caller as a string.
     *
     * @param dataSet The name of the dataset.
     */
    public String loadOverrideDoc(String infoDir, String dataSet) throws DAP2Exception {

        String userDoc = "";
        String overrideFile = dataSet + ".ovr";

        //Try to open and read the override file for this dataset.
        try {
            File fin = new File(infoDir + overrideFile);
            BufferedReader svIn = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));

            boolean done = false;

            while (!done) {
                String line = svIn.readLine();
                if (line == null) {
                    done = true;
                } else {
                    userDoc += line + "\n";
                }
            }
            svIn.close();

        } catch (FileNotFoundException fnfe) {
            userDoc += "<h2>No Could Not Open Override Document.</h2><hr>";
            return (null);
        } catch (IOException ioe) {
            throw(new DAP2Exception(opendap.dap.DAP2Exception.UNKNOWN_ERROR, ioe.getMessage()));
        }

        return (userDoc);
    }
    /***************************************************************************/

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     */

    private String get_user_supplied_docs(String serverName, String dataSet) throws DAP2Exception {

        String userDoc = "";

        //Try to open and read the Dataset specific information file.
        try {
            File fin = new File(infoDir + dataSet + ".html");
            BufferedReader svIn = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));

            boolean done = false;

            while (!done) {
                String line = svIn.readLine();
                if (line == null) {
                    done = true;
                } else {
                    userDoc += line + "\n";
                }
            }
            svIn.close();

        } catch (FileNotFoundException fnfe) {
            userDoc += "<h2>No Dataset Specific Information Available.</h2><hr>";
        } catch (IOException ioe) {
            throw(new DAP2Exception(opendap.dap.DAP2Exception.UNKNOWN_ERROR, ioe.getMessage()));
        }

        userDoc += "<hr>\n";

        //Try to open and read the server specific information file.
        try {
            String serverFile = infoDir + serverName + ".html";
            if (_Debug) System.out.println("Server Info File: " + serverFile);
            File fin = new File(serverFile);
            BufferedReader svIn = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));

            boolean done = false;

            while (!done) {
                String line = svIn.readLine();
                if (line == null) {
                    done = true;
                } else {
                    userDoc += line + "\n";
                }
            }
            svIn.close();

        } catch (FileNotFoundException fnfe) {
            userDoc += "<h2>No Server Specific Information Available.</h2><hr>";
        } catch (IOException ioe) {
            throw(new DAP2Exception(opendap.dap.DAP2Exception.UNKNOWN_ERROR, ioe.getMessage()));
        }


        return (userDoc);
    }
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private String buildGlobalAttributes(DAS das, ServerDDS dds) {

        boolean found = false;
        String ga;

        ga = "<h3>Dataset Information</h3>\n<table>\n";


        Enumeration edas = das.getNames();

        while (edas.hasMoreElements()) {
            String name = (String) edas.nextElement();


            if (!dasTools.nameInKillFile(name) &&
                    (dasTools.nameIsGlobal(name) || !dasTools.nameInDDS(name, dds))) {

                try {
                    AttributeTable attr = das.getAttributeTable(name);

                    Enumeration e = attr.getNames();
                    while (e.hasMoreElements()) {
                        String aName = (String) e.nextElement();
                        Attribute a = attr.getAttribute(aName);

                        found = true;

                        ga += "\n<tr><td align=right valign=top><b>";
                        ga += aName + "</b>:</td>\n";
                        ga += "<td align=left>";

                        Enumeration es = a.getValues();
                        while (es.hasMoreElements()) {
                            String val = (String) es.nextElement();
                            ga += val + "<br>";
                        }

                        ga += "</td></tr>\n";

                    }
                } catch (NoSuchAttributeException nsae) {
                }

            }
        }
        ga += "</table>\n<p>\n";

        if (!found)
            ga = "";

        return (ga);
    }


    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private String buildVariableSummaries(DAS das, ServerDDS dds) {


        String vs = "<h3>Variables in this Dataset</h3>\n<table>\n";

        Enumeration e = dds.getVariables();

        while (e.hasMoreElements()) {

            BaseType bt = (BaseType) e.nextElement();

            vs += "<tr>";

            vs += summarizeVariable(bt, das);

            vs += "</tr>";

        }

        vs += "</table>\n<p>\n";
        ;


        return (vs);
    }


    private String summarizeAttributes(AttributeTable attr, String vOut) {

        if (attr != null) {

            Enumeration e = attr.getNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();


                Attribute a = attr.getAttribute(name);
                if (a != null) {

                    if (a.isContainer()) {

                        vOut += "<li> <b> " + name + ": </b> </li>\n";
                        vOut += "<ul>\n";

                        try {
                            vOut += summarizeAttributes(a.getContainer(), "");
                        } catch (NoSuchAttributeException nase) {
                        }

                        vOut += "</ul>\n";

                    } else {
                        vOut += "<li> <b> " + name + ": </b> ";
                        try {
                            Enumeration es = a.getValues();
                            while (es.hasMoreElements()) {
                                String val = (String) es.nextElement();
                                vOut += val;
                                if (es.hasMoreElements())
                                    vOut += ", ";
                            }
                        } catch (NoSuchAttributeException nase) {
                        }
                        vOut += " </li>\n";
                    }

                }
            }
        }

        return (vOut);

    }


    private String summarizeVariable(BaseType bt, DAS das) {

        String vOut;

        vOut = "<td align=right valign=top><b>" + bt.getName();
        vOut += "</b>:</td>\n";
        vOut += "<td align=left valign=top>" + dasTools.fancyTypeName(bt);


        try {
            AttributeTable attr = das.getAttributeTable(bt.getName());

            // This will display the DAS variables (attributes) as a bulleted list.
            String s = "";
            vOut += "\n<ul>\n";
            vOut += summarizeAttributes(attr, s);
            vOut += "\n</ul>\n";


            if (bt instanceof DConstructor) {
                vOut += "<table>\n";

                DConstructor dc = (DConstructor) bt;

                Enumeration e = dc.getVariables();

                while (e.hasMoreElements()) {
                    BaseType bt2 = (BaseType) e.nextElement();
                    vOut += "<tr>\n";
                    vOut += summarizeVariable(bt2, das);
                    vOut += "</tr>\n";
                }
                vOut += "</table>\n";


            } else if (bt instanceof DVector) {


                DVector da = (DVector) bt;
                PrimitiveVector pv = da.getPrimitiveVector();

                if (pv instanceof BaseTypePrimitiveVector) {
                    BaseType bt2 = pv.getTemplate();

                    if (bt2 instanceof DArray || bt2 instanceof DString) {
                    } else {

                        vOut += "<table>\n";
                        vOut += "<tr>\n";
                        vOut += summarizeVariable(bt2, das);
                        vOut += "</tr>\n";
                        vOut += "</table>\n";
                    }
                }


            } else {
/*

                In the C++ code the types are all checked here, and if an unknown
                type is recieved then an exception is thrown. I think it's not
                needed... James?

                ndp


                      default:
	                assert("Unknown type" && false);
                    }
                */

                // While fixing another problem I decided that this should be
                // moved to the ed of the method...
                //vOut += "</td>\n";
            }
        } catch (NoSuchAttributeException nsae) {
        }

        // While fixing another problem I decided that this should be
        // here to close the opening tag <td> from the top of this method.
        vOut += "</td>\n";

        return (vOut);

    }

    /***************************************************************************/


}





