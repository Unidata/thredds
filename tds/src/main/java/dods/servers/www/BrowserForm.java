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
import dods.dap.DAS;



/** 
 */
public interface BrowserForm {


    /**
    * Returns an string representation of the variables value. This
    * is really foreshadowing functionality for Server types, but
    * as it may come in useful for clients it is added here. Simple 
    * types (example: DFloat32) will return a single value. DConstuctor
    * and DVector types will be flattened. DStrings and DURL's will 
    * have double quotes around them.
    */
    public void printBrowserForm(PrintWriter pw, DAS das);

 

}
