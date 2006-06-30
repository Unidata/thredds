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


package dods.servers.www;

import java.io.*;
import java.util.Enumeration;
import dods.dap.*;
import dods.util.*;


/**
 */
public class wwwOutPut  {

    private static boolean _Debug = false;

    private int _attrRows;
    private int _attrCols;
    private PrintWriter pWrt;


    public wwwOutPut(PrintWriter sink, int rows, int cols){
        _attrRows = rows;
	_attrCols = cols;
	pWrt = sink;
    }

    public wwwOutPut(PrintWriter sink){
        this(sink,5,70);
    }



    public static String nameForJsCode(String dodsName){
        return("dods_"+dodsName);
    }


    public void writeDisposition(String requestURL){
        // To get the size to be a function of the image window size, you need to
        // use some JavaScript code to generate the HTML. C++ --> JS --> HTML.
        // 4/8/99 jhrg

        pWrt.print( "<tr>"
            + "<td align=\"right\">\n"
            + "<h3><a href=\"dods_form_help.html#disposition\" valign=\"bottom\">Action:</a></h3>\n"
            + "<td><input type=\"button\" value=\"Get ASCII\" onclick=\"ascii_button()\">\n"
            + "<input type=\"button\" value=\"Get Binary\" onclick=\"binary_button()\">\n"
//            + "<input type=\"button\" value=\"Send to Program\" onclick=\"program_button()\">\n" // removed 9/17/01 ndp
            + "<input type=\"button\" value=\"Show Help\" onclick=\"help_button()\">\n"
            + "<tr>\n"
            + "<td align=\"right\">\n"
	    + "<h3><a href=\"dods_form_help.html#data_url\" valign=\"bottom\">Data URL:</a></h3>"
	    + "<td><input name=\"url\" type=\"text\" size="
	    + _attrCols
	    + " value=\""
	    + requestURL + "\">\n"
	    );
    }


    public void writeGlobalAttributes(DAS das, DDS dds){

        pWrt.print( "<tr>\n"
            + "<td align=\"right\" valign=\"top\">\n"
            + "<h3><a href=\"dods_form_help.html#global_attr\">Global Attributes:</a></h3>\n"
            + "<td><textarea name=\"global_attr\" rows="
            + _attrRows
            + " cols="
            + _attrCols
            + ">\n"
            );


        Enumeration edas = das.getNames();

        while(edas.hasMoreElements()){
	    String name = (String)edas.nextElement();


	    if(!dasTools.nameInKillFile(name)   &&
	        (dasTools.nameIsGlobal(name) || !dasTools.nameInDDS(name, dds))      ){

                AttributeTable attr = das.getAttributeTable(name);

                writeAttributes(attr);
            }
        }

        pWrt.print("</textarea><p>\n\n");
    }




    public void writeAttributes(AttributeTable aTbl) {
        writeAttributes(aTbl,"");
    }

    public void writeAttributes(AttributeTable aTbl, String indent) {

        if(aTbl != null){

            Enumeration e = aTbl.getNames();

            while(e.hasMoreElements()){
                String aName = (String)e.nextElement();
                Attribute a = aTbl.getAttribute(aName);


                if (a.isContainer()) {
                    pWrt.print(indent+aName+":\n");
                    writeAttributes(a.getContainer(),indent+"  ");
                }
                else {
                    pWrt.print(indent + aName + ": ");

                    if(_Debug) { System.out.println("Getting attribute value enumeration for \""+aName +"\"...");}
                    Enumeration es = a.getValues();
                    if(_Debug) { System.out.println("Attribute Values enumeration: "+es);}
                    int i = 0;
                    while(es.hasMoreElements()){
                        String val = (String)es.nextElement();
                        if(_Debug) { System.out.println("Value " + i + ": "+val);}

                        pWrt.print(val);

                        if(es.hasMoreElements())
                        pWrt.print(", ");

                        i++;
                    }
                    pWrt.println("");
                }

            }
        }
    }

    /*
    void
    WWWOutput::write_variable_entries(DAS &das, DDS &dds)
    {
        // This writes the text `Variables:' and then sets up the table so that
        // the first variable's section is written into column two.
        _os << \
    "
    <tr>
    <td align=\"right\" valign=\"top\">
    <h3><a href=\"dods_form_help.html#dataset_variables\">Variables:</a></h3>
    <td>";

        for (Pix p = dds.first_var(); p; dds.next_var(p)) {
	    dds.var(p)->print_val(_os);
	    write_variable_attributes(dds.var(p), das);
	    _os << "\n<p><p>\n\n";		// End the current var's section
	    _os << "<tr><td><td>\n\n";	// Start the next var in column two
        }
    }
    */
    public void writeVariableEntries(DAS das, DDS dds){
        // This writes the text `Variables:' and then sets up the table
        // so that the first variable's section is written into column two.

        pWrt.print("<tr>\n"
            + "<td align=\"right\" valign=\"top\">\n"
            + "<h3><a href=\"dods_form_help.html#dataset_variables\">Variables:</a></h3>\n"
            + "<br><td>\n"
	    );  
    
        Enumeration e = dds.getVariables(); 
	
	while(e.hasMoreElements()){
	    BaseType bt = (BaseType)e.nextElement();

	    ((BrowserForm)bt).printBrowserForm(pWrt,das);

	    writeVariableAttributes(bt, das);

	    pWrt.print("\n<p><p>\n\n"); // End the current var's section
	    pWrt.print("<tr><td><td>\n\n"); // Start the next var in column two
       }


    }



    /*
    void
    WWWOutput::write_variable_attributes(BaseType *btp, DAS &das)
    {
        AttrTable *attr = das.get_table(btp->name());
        // Don't write anything if there are no attributes.
        if (!attr)
	    return;

        _os << "<textarea name=\"" << btp->name() << "_attr" << "\" rows="
	    << _attr_rows << " cols=" << _attr_cols << ">\n";
        write_attributes(attr);
        _os << "</textarea>\n\n";
    }
    */
    public void writeVariableAttributes(BaseType bt, DAS das){

        AttributeTable attr = das.getAttributeTable(bt.getName());

        if(attr!=null) {

	    pWrt.print( "<textarea name=\""
                + bt.getLongName().replace('.','_')
                + "_attr"
                + "\" rows="
                + _attrRows
                + " cols="
                + _attrCols
                + ">\n"
                );

	    writeAttributes(attr);
            pWrt.print( "</textarea>\n\n");

        }
    }





    public void writeSimpleVar(PrintWriter pw, BaseType bt){

        String name = bt.getLongName().replace('.','_');
	String type = dasTools.fancyTypeName(bt);

        pWrt.print(
            "<script type=\"text/javascript\">\n"
            + "<!--\n"
            + nameForJsCode(name) + " = new dods_var(\""  
	    + bt.getLongName() + "\", \""  // This name goes into the URL that's built by the form.
	    + nameForJsCode(name) + "\", 0);\n"
            + "DODS_URL.add_dods_var("
	    + nameForJsCode(name) + ");\n"
            + "// -->\n"
            + "</script>\n"
            );

        pWrt.print(
            "<b>"
            + "<input type=\"checkbox\" name=\"get_"
	    + nameForJsCode(name) + "\"\n"
            + "onclick=\""
	    + nameForJsCode(name) + ".handle_projection_change(get_"
            + nameForJsCode(name) + ")\">\n"
            + "<font size=\"+1\">" 
	    + bt.getName() + "</font>" // this is the Bold faced name of the variable in the form
            + ": " + type + "</b><br>\n\n"
	    );


        pWrt.print(
	
            bt.getName() //this name is the one used when choosing a constraint relation (=, <, >, etc.)
	    + " <select name=\"" + nameForJsCode(name) + "_operator\""
            + " onfocus=\"describe_operator()\""
            + " onchange=\"DODS_URL.update_url()\">\n"
            + "<option value=\"=\" selected>=\n"
            + "<option value=\"!=\">!=\n"
            + "<option value=\"<\"><\n"
            + "<option value=\"<=\"><=\n"
            + "<option value=\">\">>\n"
            + "<option value=\">=\">>=\n"
            + "<option value=\"-\">--\n"
            + "</select>\n"
            );

        pWrt.print(
            "<input type=\"text\" name=\"" + nameForJsCode(name) + "_selection"
            + "\" size=12 onFocus=\"describe_selection()\" "
            + "onChange=\"DODS_URL.update_url()\">\n"
	    );

        pWrt.print( "<br>\n\n");


    }
    
   	
	



}
