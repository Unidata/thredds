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

import java.io.*;
import java.util.*;


/**
The earlier versions of the DODSServlet used .ini files to store configuration
information for the servlet. This storage has been moved from the .ini file into
the web.xml file in the WEB-INF directory for the servlet in the servlet engine.
You can use .ini files for other reasons, and the classes are still present in the
distribution, but you will have to make use of them yourself, as the DODSServlet 
no longer accesses the .ini files to get configuration information.
*/

public class DODSiniFile extends iniFile {


    private static final String fname = "DODS.ini";
    
	    
    //************************************************************************
    /**
    */
    public DODSiniFile(){
	super(null,fname,false);    
    }
    //************************************************************************
    
    
    
    //************************************************************************
    /**
    */
    public DODSiniFile(String path)  {

    
	super(path,fname,false);    
   
    }
    //************************************************************************


    //************************************************************************
    /**
    */
    public DODSiniFile(String path, boolean dbg)  {

    	super(path,fname,dbg);        
    
    }
    //************************************************************************



           
    
}


