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

// $Log: InvalidOperatorException.java,v $
// Revision 1.1  2005/12/16 22:06:59  caron
// dods src under our CVS
//
// Revision 1.8  2002/01/03 22:58:47  ndp
// Merged newClauseImplementation branch into trunk.
//
// Revision 1.7.2.1  2002/01/03 19:14:51  ndp
// New Clause and Function stuff working...
//
// Revision 1.7  2001/02/04 01:44:48  ndp
// Cleaned up javadoc errors
//
// Revision 1.6  1999/09/24 23:17:58  ndp
// Added new constructors to all Exception classes
//
// Revision 1.5  1999/09/24 21:59:23  ndp
// Reorged Exceptions. Code compiles.
//
// Revision 1.4  1999/09/16 21:22:52  ndp
// *** empty log message ***
//
// Revision 1.3  1999/08/20 22:59:43  jimg
// Changed the package declaration to dods.dap.Server so that it matches the
// directory name.
//

package dods.dap.Server;
import dods.dap.DODSException;

/**
 * Thrown when a <code>RelOp</code> operation is called
 * on two types for which it makes no sense to compre, such as
 * attempting to ascertain is a String is less than a Float.
 *
 * @version $Revision: 51 $
 * @author ndp
 */
public class InvalidOperatorException extends SDODSException {
  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public InvalidOperatorException(String s) {
    super(DODSException.MALFORMED_EXPR,"Invalid Operator Exception: " + s);
  }


  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * message and DODS error code (see <code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public InvalidOperatorException(int err, String s) {
    super(err,s);
  }
}
