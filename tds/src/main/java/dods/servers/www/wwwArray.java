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

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import dods.dap.*;
import dods.util.*;

/**
 */
public class wwwArray extends DArray implements BrowserForm {

    private static boolean _Debug = false;

    /** Constructs a new <code>asciiArray</code>. */
    public wwwArray() {
        this(null);
    }

    /**
    * Constructs a new <code>asciiArray</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public wwwArray(String n) {
        super(n);
    }

    public void printBrowserForm(PrintWriter pw, DAS das){
    
    
        /*--------------------------------------------------------
	// C++ implementation looks like this...
	
        os << "<script type=\"text/javascript\">\n"
           << "<!--\n"
           << name_for_js_code(name()) 
           << " = new dods_var(\"" 
           << name() 
           << "\", \"" 
           << name_for_js_code(name()) 
           << "\", 1);\n"
           << "DODS_URL.add_dods_var(" 
           << name_for_js_code(name()) 
           << ");\n"
           << "// -->\n"
           << "</script>\n";

        os << "<b>" 
           << "<input type=\"checkbox\" name=\"get_" 
           << name_for_js_code(name())
           << "\"\n"
           << "onclick=\"" 
           << name_for_js_code(name())
           << ".handle_projection_change(get_"
           << name_for_js_code(name()) 
           << ")\">\n" 
           << "<font size=\"+1\">" 
           << name() 
           << "</font>"
           << ": " 
           << fancy_typename(this) 
           << "</b><br>\n\n";

        Pix p = first_dim();
        for (int i = 0; p; ++i, next_dim(p)) {
	    int size = dimension_size(p, true);
	    string n = dimension_name(p);
	    if (n != "")
	        os << n << ":";
	    os << "<input type=\"text\" name=\"" 
	       << name_for_js_code(name())
	       << "_" 
	       << i 
	       << "\" size=8 onfocus=\"describe_index()\""
	       << " onChange=\"DODS_URL.update_url()\">\n";
	   
	    os << "<script type=\"text/javascript\">\n"
	       << "<!--\n"
	       << name_for_js_code(name()) 
	       << ".add_dim(" 
	       << size 
	       << ");\n"
	       << "// -->\n"
	       << "</script>\n";
        }
    
        os << "<br>\n\n";

        --------------------------------------------------------*/


        pw.print(
            "<script type=\"text/javascript\">\n"
            + "<!--\n"
            + wwwOutPut.nameForJsCode(getName()) 
            + " = new dods_var(\"" 
            + getName()
	    +  "\", \""
	    + wwwOutPut.nameForJsCode(getName())
            + "\", 1);\n"
            + "DODS_URL.add_dods_var(" 
            + wwwOutPut.nameForJsCode(getName()) 
            + ");\n"
            + "// -->\n"
            + "</script>\n"
            );

        pw.print(
          "<b>" 
           + "<input type=\"checkbox\" name=\"get_" 
	   + wwwOutPut.nameForJsCode(getName()) 
	   + "\"\n"
           + "onclick=\"" 
	   + wwwOutPut.nameForJsCode(getName()) 
	   + ".handle_projection_change(get_"
           + wwwOutPut.nameForJsCode(getName()) 
	   + ")\">\n" 
           + "<font size=\"+1\">" 
	   + getName() 
	   + "</font>"
           + ": " 
	   + dasTools.fancyTypeName(this) 
	   + "</b><br>\n\n"
	   );

        int dims = numDimensions();
        int dimSize;
	int i = 0;
        String dimName;
        for (Enumeration e = getDimensions(); e.hasMoreElements(); ) {
            DArrayDimension d = (DArrayDimension)e.nextElement();
            dimSize = d.getSize();
            dimName = d.getName();
	    
	    if(dimName != null)
	        pw.print(dimName + ":");

            pw.print(
                "<input type=\"text\" name=\"" 
                + wwwOutPut.nameForJsCode(getName()) 
                + "_" 
                + i 
                + "\" size=8 onfocus=\"describe_index()\""
                + " onChange=\"DODS_URL.update_url()\">\n"
                );

            pw.print(
                "<script type=\"text/javascript\">\n"
                + "<!--\n"
                + wwwOutPut.nameForJsCode(getName())
                + ".add_dim(" 
                + dimSize 
                + ");\n"
                + "// -->\n"
                + "</script>\n"
                );
		
	    i++;
        }		


        pw.println("<br>\n");
    }


}
