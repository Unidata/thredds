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

// $Log: SDODSException.java,v $
// Revision 1.1  2005/12/16 22:07:01  caron
// dods src under our CVS
//
// Revision 1.4  2001/02/04 01:44:48  ndp
// Cleaned up javadoc errors
//
// Revision 1.3  1999/09/24 21:59:24  ndp
// Reorged Exceptions. Code compiles.
//
// Revision 1.2  1999/08/20 22:58:18  jimg
// Change the package declaration to dods.dap.Server so that it would match the
// directory name (which I think Java requires).
// Added the import of DODSException.
//

package dods.dap.Server;
import dods.dap.DODSException;

/**
 * SDODS exception. This is the root of all the DODS Server exception classes.
 *
 * @version $Revision$
 * @author ndp
 */
public class SDODSException extends DODSException {
  /**
   * Construct a <code>SDODSException</code> with the specified detail
   * message.
   *
   * @param s the detail message.
   */
  public SDODSException(int err, String s) {
    super(err,s);
  }
}
