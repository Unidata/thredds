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
import java.io.DataInputStream;
import java.io.PrintWriter;
import dods.dap.*;

/**
 */
public class wwwList extends DList implements BrowserForm {
  
    private static boolean _Debug = false;

     /** Constructs a new <code>wwwList</code>. */
    public wwwList() {
        this(null);
    }

    /**
    * Constructs a new <code>wwwList</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public wwwList(String n) {
        super(n);
    }
    
    public void printBrowserForm(PrintWriter pw, DAS das){
    
        /*-----------------------------------------------
        // C++ implementation contains only this line...

         var()->print_val(os, "", print_decl_p);
        -----------------------------------------------*/

        PrimitiveVector pv = getPrimitiveVector();
	
	BaseType bt = pv.getTemplate();
	
	((BrowserForm)bt).printBrowserForm(pw, das);


    }

    

  
}
