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
import java.util.Enumeration;
import java.util.Vector;
import dods.dap.*;

/**
 */
public class asciiSeq extends DSequence implements toASCII {

    private static boolean _Debug = false;

     /** Constructs a new <code>asciiSeq</code>. */
    public asciiSeq() {
        this(null);
    }

    /**
    * Constructs a new <code>asciiSeq</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public asciiSeq(String n) {
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
       
        if(_Debug) System.out.println("asciiSeq.toASCII("+addName+",'"+rootName+"')  getName(): "+getName());
	//System.out.println("this: " + this + " Has "+allValues.size() + " elements.");

        if(rootName != null)
	    rootName += "." + getName();
	else
	    rootName = getName();

        pw.print( toASCIIFlatName(rootName));		
	             
/*
        for(Enumeration e1 = allValues.elements(); e1.hasMoreElements(); ) {
            // get next instance vector
            Vector v = (Vector)e1.nextElement();
            for(Enumeration e2 = v.elements(); e2.hasMoreElements(); ) {
                // get next instance variable
                BaseType bt = (BaseType)e2.nextElement();
		
		pw.print(bt.toASCIIFlatName(rootName)+",");		
            }
	    break;
        }
*/	
	
        pw.println("");		

        int i = 0;
        for(Enumeration e1 = allValues.elements(); e1.hasMoreElements(); ) {
	    
	    int j = 0;
            // get next instance vector
            Vector v = (Vector)e1.nextElement();
            for(Enumeration e2 = v.elements(); e2.hasMoreElements(); ) {
                // get next instance variable
                toASCII ta = (toASCII)e2.nextElement();

                if(j>0) pw.print(", ");
                ta.toASCII(pw, false, rootName, false);
		j++;	
            }
	    pw.println("");
        }

              
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
    
        String s = "";
	boolean firstPass = true;
        for(Enumeration e1 = allValues.elements(); e1.hasMoreElements(); ) {
            // get next instance vector
            Vector v = (Vector)e1.nextElement();
            for(Enumeration e2 = v.elements(); e2.hasMoreElements(); ) {
                // get next instance variable
                toASCII ta = (toASCII)e2.nextElement();
		
		if(!firstPass)
		    s += ", ";
		s += ta.toASCIIFlatName(rootName);
		firstPass = false;		
            }
	    break;
        }
	return(s);
    }

}
