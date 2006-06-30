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


package dods.servers.ascii;
import java.io.*;
import dods.dap.*;

/**
 */
public class asciiBool extends DBoolean implements toASCII {

    private static boolean _Debug = false;

    /** Constructs a new <code>asciiBool</code>. */
    public asciiBool() {
        this(null);
    }

    /**
    * Constructs a new <code>asciiBool</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public asciiBool(String n) {
        super(n);
    }


  /**
   * Returns a string representation of the variables value. This
   * is really foreshadowing functionality for Server types, but
   * as it may come in useful for clients it is added here. Simple 
   * types (example: DFloat32) will return a single value. DConstuctor
   * and DVector types will be flattened. DStrings and DURL's will 
   * have double quotes around them.
   *
   * @param addName is a flag indicating if the variable name should
   * appear at the begining of the returned string.
   */
   public void toASCII(PrintWriter pw,
                       boolean addName,
		       String rootName,
		       boolean newLine){
       
        rootName = toASCIIAddRootName(pw,addName, rootName);
       
        if(addName)
            pw.print(", ");
	            
       pw.print((new Boolean(getValue())).toString());
       
       if(newLine)
           pw.print("\n");
       
   }

    public String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName){
    
        if(addName){
            rootName = toASCIIFlatName(rootName);
            pw.print(rootName);
        }	
	return(rootName);

    }

    public String toASCIIFlatName(String rootName){
        String s;
        if(rootName != null){
            s = rootName +  "." + getName();
        }
        else {
            s = getName();
        }
	return(s);
    }




}
