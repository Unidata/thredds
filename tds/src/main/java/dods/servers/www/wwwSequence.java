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
import java.util.Vector;
import dods.dap.*;

/**
 */
public class wwwSequence extends DSequence implements BrowserForm {

    private static boolean _Debug = false;

     /** Constructs a new <code>wwwSeq</code>. */
    public wwwSequence() {
        this(null);
    }

    /**
    * Constructs a new <code>wwwSeq</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public wwwSequence(String n) {
        super(n);
    }
    
    public void printBrowserForm(PrintWriter pw, DAS das){


        /*-----------------------------------------------------
        // C++ implementation looks like this...

        os << "<b>Sequence " << name() << "</b><br>\n";
        os << "<dl><dd>\n";

        for (Pix p = first_var(); p; next_var(p)) {
            var(p)->print_val(os, "", print_decls);
            wo.write_variable_attributes(var(p), global_das);
            os << "<p><p>\n";
        }

        os << "</dd></dl>\n";
        -----------------------------------------------------*/

        pw.print(
            "<b>Sequence " 
            + getName() 
            + "</b><br>\n"
            + "<dl><dd>\n"
            );

        wwwOutPut wOut = new wwwOutPut(pw);
	
        Enumeration e = getVariables();	
	while(e.hasMoreElements()){
	    BaseType bt = (BaseType)e.nextElement();
	    
	    ((BrowserForm)bt).printBrowserForm(pw, das);
	    	    
	    wOut.writeVariableAttributes(bt, das);
            pw.print("<p><p>\n");

	}
        pw.println("</dd></dl>\n");
	
    }

    


}
