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



package dods.util;

import java.util.Enumeration;
import java.io.*;

import dods.dap.*;

/**
 */
public class dasTools {


     
    /** This code could use a real `kill-file' some day - 
    *   about the same time that the rest of the server gets 
    *   an `rc' file... For the present just return
    *   false (There is no killing going on here...)
    *
    *   The C++ implementation looks like this:
    *
    *   static bool
    *   name_in_kill_file(const string &name)
    *   {
    *       static Regex dim(".*_dim_[0-9]*", 1); // HDF `dimension' attributes.
    *
    *       return dim.match(name.c_str(), name.length()) != -1;
    *   }
    *
    */
    public static boolean nameInKillFile(String name){
        return(false);
    }




    public static boolean nameInDDS(String name, DDS dds){

	boolean found = true;
	
        try { 
	    dds.getVariable(name);
	}
	catch (NoSuchVariableException e) {
	
	    found = false;
	}
	
        //System.out.println("nameInDDS(): "+found);
        return(found);
    }




    /* C++ implementation
    static bool
    name_is_global(string &name)
    {
        static Regex global("\\(.*global.*\\)\\|\\(.*dods.*\\)", 1);
        downcase(name);
        return global.match(name.c_str(), name.length()) != -1;
    }
    */
    public static boolean nameIsGlobal(String name){
    
        String lcName = name.toLowerCase();
	boolean global = false;
	
	if(lcName.indexOf("global") >= 0)
	    global = true;
	    
	if(lcName.indexOf("dods") >= 0)
	    global = true;
	
	
        //System.out.println("nameIsGlobal(): "+global);
    
        return(global);
    }





    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public static String fancyTypeName(BaseType bt){
    
        String fancy;
	
	if(bt instanceof DByte)
	    return("8 bit Byte");

	if(bt instanceof DUInt16)
	    return("16 bit Unsigned Integer");
		
	if(bt instanceof DInt16)
	    return("16 bit Integer");
		
	if(bt instanceof DUInt32)
	    return("32 bit Unsigned Integer");
	
	if(bt instanceof DInt32)
	    return("32 bit Integer");
		
	if(bt instanceof DFloat32)
	    return("32 bit Real");
		
	if(bt instanceof DFloat64)
	    return("64 bit Real");
		
	if(bt instanceof DURL)
	    return("URL");
	    
	if(bt instanceof DString)
	    return("String");
		
		
	if(bt instanceof DArray){
	
	    DArray a = (DArray) bt;
	    String type = "Array of " + 
	                  fancyTypeName(a.getPrimitiveVector().getTemplate()) + 
			  "s ";
            
	    Enumeration e = a.getDimensions();
	    while(e.hasMoreElements()){
	        DArrayDimension dad = (DArrayDimension)e.nextElement();
	
	        type += "[" + dad.getName() + " = 0.." + (dad.getSize()-1) +"]";
	
	    }
	    type += "\n";
	    return(type);
	}

	if(bt instanceof DList){
	    DList a = (DList) bt;
	    String type = "List of " + 
	                  fancyTypeName(a.getPrimitiveVector().getTemplate()) + 
			  "s\n";
            
	   return(type);
	}
	
	if(bt instanceof DStructure)
	    return("Structure");
		
	if(bt instanceof DSequence)
	    return("Sequence");
		
	if(bt instanceof DGrid)
	    return("Grid");

	return("UNKNOWN");
  

    }
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

 
  
  
  
}
