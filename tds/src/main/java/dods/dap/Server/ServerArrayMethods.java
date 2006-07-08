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
//
// Based on source code and instructions from the work of:
//
/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////


package dods.dap.Server;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import dods.dap.BaseType;
//import dods.dap.NoSuchVariableException;
 
 
/** This interface extends the <code>ArrayMethods</code> for DODS types that
  * extend <code>DArray</code> and <code>DGrid</code> classes. It contains 
  * additional projection methods needed by the Server side implementations  
  * of these types.
  * <p>A projection for an array must include the start, stride and stop
  * information for each dimension of the array in addition to the basic
  * information that the array itself is <em>projected</em>. This interface
  * provides access to that information.
  * @see dods.dap.DArray
  * @see dods.dap.DGrid
  * @see dods.dap.Server.SDArray
  * @see dods.dap.Server.SDGrid
  * @see dods.dap.Server.ServerMethods
  * @see dods.dap.Server.Operator
    
  * @version $Revision: 1.1 $
  * @author jhrg & ndp */


public interface ServerArrayMethods extends ServerMethods {
  
    public void  setProjection(int dimension,int start, int stride, int stop) 
	throws InvalidParameterException, SBHException;

    public int   getStart (int dimension) throws InvalidParameterException;
 
    public int   getStride(int dimension) throws InvalidParameterException;

    public int   getStop  (int dimension) throws InvalidParameterException;
        
}
