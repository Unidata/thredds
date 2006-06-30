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

/* $Id: dodsINFO.java,v 1.1 2005/12/16 22:37:05 caron Exp $
*
*/

package dods.servlet;

import java.io.*;
//import java.text.*;
import java.util.*;
//import java.util.zip.DeflaterOutputStream;
//import javax.servlet.*;
//import javax.servlet.http.*;

import dods.dap.*;
import dods.util.*;
import dods.dap.Server.*;
import dods.dap.parser.ParseException;

/**
* Default handler for DODS info requests. This class is used
* by DODSServlet. This code exists as a seperate class in order to alleviate
* code bloat in the DODSServlet class. As such, it contains virtually no
* state, just behaviors.
*
* @author Nathan David Potter
*/

public class dodsINFO {

    private static final boolean _Debug = true;

    private String infoDir = null;

    /***************************************************************************
    * Default handler for DODS info requests. Returns an html document
    * describing the contents of the servers datasets.
    *
    * The "info_cache_dir" directory specified in the [Server] section
    * of the DODSiniFile is the designated location for:
    * <ul>
    *     <li>".info" response override files.</li>
    *     <li>Server specific HTML* files.</li>
    *     <li>Dataset specific HTML* files .</li>
    * </ul>
    *
    * The server specific HTML* files must be named #servlet#.html
    * where #servlet# is the name of the servlet that is running as
    * the DODS server in question. This name is determined at run time
    * by using the class called Class ( this.getClass().getName() ).
    *
    * <p>In the C++ code the analogy is the per-cgi file names.</p>
    *
    * <p>
    * The dataset specific HTML* files are located by catenating `.html'
    * to #name#, where #name# is the name of the dataset. If the filename part
    * of #name# is of the form [A-Za-z]+[0-9]*.* then this function also looks
    * for a file whose name is [A-Za-z].html For example, if #name# is
    * .../data/fnoc1.nc this function first looks for .../data/fnoc1.nc.html.
    * However, if that does not exist it will look for .../data/fnoc.html. This
    * allows one `per-dataset' file to be used for a collection of files with
    * the same root name.
    * </p>
    *
    * NB: An HTML* file contains HTML without the <html>, <head> or <body> tags
    * (my own notation).
    *
    * <b>TODO Look for the user supplied Server- and dataset-specific HTML* documents.</b>
    *
    * @param pw The PrintStream to which the output should be written.
    * @param gds The datset.
    * @param rs The ReqState object for this client request.
    */
    public void sendINFO(PrintStream pw, GuardedDataset gds, ReqState rs) throws DODSException, ParseException {

        if(_Debug) System.out.println("dods.servlet.dodsINFO.sendINFO() reached.");

        String responseDoc = null;
	ServerDDS    myDDS = null;
        DAS          myDAS = null;


        myDDS = (ServerDDS) gds.getDDS();
        myDAS = gds.getDAS();



        infoDir = rs.getInitParameter("INFOcache");
	if(infoDir == null)
	    infoDir = rs.getINFOCache();



	responseDoc = loadOverrideDoc(infoDir, rs.getDataSet());

        if( responseDoc != null ){
	    if(_Debug) System.out.println("override document: " + responseDoc);
            pw.print(responseDoc);
        }
        else {


            String user_html = get_user_supplied_docs(rs.getServerName(), rs.getDataSet());

            String global_attrs = buildGlobalAttributes(myDAS, myDDS);

            String variable_sum = buildVariableSummaries(myDAS, myDDS);




	    // Send the document back to the client.
	    pw.println("<html><head><title>Dataset Information</title>");
	    pw.println("<style type=\"text/css\">");
            pw.println("<!-- ul {list-style-type: none;} -->");
            pw.println("</style>");
	    pw.println("</head>");
            pw.println("<body>");

            if (global_attrs.length()>0) {
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



    /***************************************************************************
    * Checks the info directory for user supplied override documents for the
    * passed dataset name. If there are overridedocuments present then the
    * contents are read and returned to the caller as a string.
    *
    *
    * @param dataSet The name of the dataset.
    *
    */
    public String loadOverrideDoc(String infoDir, String dataSet) throws DODSException{

        String userDoc      = "";
        String overrideFile = dataSet + ".ovr";


	//Try to open and read the override file for this dataset.
        try {
	    File fin = new File(infoDir + overrideFile);
            BufferedReader svIn = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));

	    boolean done = false;

	    while(!done) {
	        String line = svIn.readLine();
		if(line == null){
		    done = true;
		}
		else {
		    userDoc += line + "\n";
		}
	    }
	    svIn.close();

	}
        catch (FileNotFoundException fnfe) {
            userDoc += "<h2>No Could Not Open Override Document.</h2><hr>";
            return(null);
        }
        catch (IOException ioe) {
            throw( new DODSException(DODSException.UNKNOWN_ERROR,ioe.getMessage()));
        }

        return(userDoc);
    }
    /***************************************************************************/


    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    /**
    */

    private String get_user_supplied_docs(String serverName, String dataSet) throws DODSException {

        String userDoc     = "";

	//Try to open and read the Dataset specific information file.
        try {
	    File fin = new File(infoDir + dataSet + ".html");
            BufferedReader svIn = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));

	    boolean done = false;

	    while(!done) {
	        String line = svIn.readLine();
		if(line == null){
		    done = true;
		}
		else {
		    userDoc += line + "\n";
		}
	    }
	    svIn.close();

	}
        catch (FileNotFoundException fnfe) {
            userDoc += "<h2>No Dataset Specific Information Available.</h2><hr>";
        }
        catch (IOException ioe) {
            throw( new DODSException(DODSException.UNKNOWN_ERROR,ioe.getMessage()));
        }

        userDoc += "<hr>\n";


	//Try to open and read the server specific information file.
        try {
	    String serverFile = infoDir + serverName + ".html";
	    if(_Debug) System.out.println("Server Info File: "+serverFile);
	    File fin = new File(serverFile);
            BufferedReader svIn = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));

	    boolean done = false;

	    while(!done) {
	        String line = svIn.readLine();
		if(line == null){
		    done = true;
		}
		else {
		    userDoc += line + "\n";
		}
	    }
	    svIn.close();

	}
        catch (FileNotFoundException fnfe) {
            userDoc += "<h2>No Server Specific Information Available.</h2><hr>";
        }
        catch (IOException ioe) {
            throw( new DODSException(DODSException.UNKNOWN_ERROR,ioe.getMessage()));
        }


        return(userDoc);
    }
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private String buildGlobalAttributes(DAS das, ServerDDS dds){

        boolean found = false;
	String ga;

	ga =  "<h3>Dataset Information</h3>\n<table>\n";


        Enumeration edas = das.getNames();

        while(edas.hasMoreElements()){
	    String name = (String)edas.nextElement();


	    if(!dasTools.nameInKillFile(name)   &&
	        (dasTools.nameIsGlobal(name) || !dasTools.nameInDDS(name, dds))      ){

                AttributeTable attr = das.getAttributeTable(name);

                if(attr != null){

	            Enumeration e = attr.getNames();
	            while(e.hasMoreElements()){
	                String aName = (String)e.nextElement();
		        Attribute a = attr.getAttribute(aName);

			found = true;

			ga += "\n<tr><td align=right valign=top><b>";
			ga += aName + "</b>:</td>\n";
			ga += "<td align=left>";

                        Enumeration es = a.getValues();
		        while(es.hasMoreElements()){
		            String val = (String)es.nextElement();
			    ga += val + "<br>";
		        }
		        ga += "</td></tr>\n";

	            }
	        }
            }
        }
	ga += "</table>\n<p>\n";

        if(!found)
	    ga = "";

	return(ga);
    }



    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    private String buildVariableSummaries(DAS das, ServerDDS dds){



        String vs = "<h3>Variables in this Dataset</h3>\n<table>\n";

        Enumeration e = dds.getVariables();

        while( e.hasMoreElements() ) {

	        BaseType bt = (BaseType)e.nextElement();

	        vs += "<tr>";

	        vs += summarizeVariable(bt, das);

	        vs += "</tr>";

        }

        vs += "</table>\n<p>\n";;



        return(vs);
    }



    private String summarizeAttributes(AttributeTable attr, String vOut ){

        if(attr != null){

            Enumeration e = attr.getNames();
            while(e.hasMoreElements()){
                String name = (String)e.nextElement();
                Attribute a = attr.getAttribute(name);

                if(a.isContainer()){

                    vOut += "<li> <b> "+name + ": </b> </li>\n";
                    vOut += "<ul>\n";
                    vOut += summarizeAttributes(a.getContainer(),"");
                    vOut += "</ul>\n";

                }
                else {
                   vOut += "<li> <b> " + name + ": </b> ";
                   Enumeration es = a.getValues();
                    while(es.hasMoreElements()){
                        String val = (String)es.nextElement();
                        vOut += val;
                        if(es.hasMoreElements())
                            vOut += ", ";
                    }
                    vOut += " </li>\n";
                }
            }
        }

        return(vOut);

    }




    private String summarizeVariable(BaseType bt, DAS das){

        String vOut;

        vOut  = "<td align=right valign=top><b>" + bt.getName();
        vOut += "</b>:</td>\n";
        vOut += "<td align=left valign=top>" + dasTools.fancyTypeName(bt);


        AttributeTable attr = das.getAttributeTable(bt.getName());


        // This will display the DAS variables (attributes) as a bulleted list.
        String s ="";
	vOut += "\n<ul>\n";
        vOut += summarizeAttributes(attr, s);
	vOut += "\n</ul>\n";





	    if(bt instanceof DConstructor){
	        vOut += "<table>\n";

	        DConstructor dc = (DConstructor)bt;

	        Enumeration e = dc.getVariables();

	        while(e.hasMoreElements()){
	            BaseType bt2  = (BaseType)e.nextElement();
		    vOut += "<tr>\n";
		    vOut += summarizeVariable(bt2,das);
		    vOut += "</tr>\n";
	        }
	        vOut += "</table>\n";


	    }
	    else if(bt instanceof DVector){


	        DVector da = (DVector)bt;
	        PrimitiveVector pv = da.getPrimitiveVector();

	        if(pv instanceof BaseTypePrimitiveVector){
	            BaseType bt2 = pv.getTemplate();

		    if(bt2 instanceof DArray || bt2 instanceof DString){
		    }
		    else {

	                vOut += "<table>\n";
	                vOut += "<tr>\n";
	                vOut += summarizeVariable(bt2,das);
	                vOut += "</tr>\n";
	                vOut += "</table>\n";
		    }
	        }


	    }
	    else{
                /*

                In the C++ code the types are all checked here, and if an unknown
                type is recieved then an exception is thrown. I think it's not
                needed... James?

                ndp


                      default:
	                assert("Unknown type" && false);
                    }
                */
	        vOut += "</td>\n";
	    }


	    return(vOut);

    }




    /***************************************************************************/




}



