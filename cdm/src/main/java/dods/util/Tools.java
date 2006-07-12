
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


/* $Id$
*
*/

package dods.util;

import java.lang.reflect.*;

/**
*
* @author Nathan David Potter
*/


public abstract class Tools {

    //#*******************************************************************************	
    /**	
    *	Show me lots of stuff about the passed in object
    *
    *
    */	
    public static void probeObject(Object o) {		

        Class c = o.getClass();
	
	Class interfaces[] = c.getInterfaces();
	Class parent = c.getSuperclass();
        Method m[] = c.getMethods();
	
	System.out.println("********* OBJECT PROBE *********");
	System.out.println("Class Name:  "+c.getName());
	System.out.println("Super Class: "+parent.getName());
	System.out.println("Interfaces: ");
		for(int i=0; i<interfaces.length ;i++){	
	    System.out.println("    "+interfaces[i].getName());
	}
	
	
	System.out.println("Methods:");
	for(int i=0; i<m.length ;i++){
	
	
            Class params[] = m[i].getParameterTypes();
            Class excepts[] = m[i].getExceptionTypes();
            Class ret = m[i].getReturnType();
	
	    System.out.print("    "+ret.getName() + "  "+m[i].getName()+"(");
	    
	    for(int j=0; j<params.length ; j++){
	        if(j>0)
		    System.out.print(", ");
	        System.out.print(params[j].getName());
	    }
	    System.out.print(")  throws ");
	    for(int j=0; j<excepts.length ; j++){
	        if(j>0)
		    System.out.print(", ");
	        System.out.print(excepts[j].getName());
	    }
            System.out.println("");
        }
	System.out.println("******************");

    }			
    //#*******************************************************************************	


}
