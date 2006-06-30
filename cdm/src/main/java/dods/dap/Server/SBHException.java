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

// $Log: SBHException.java,v $
// Revision 1.1  2005/12/16 22:07:00  caron
// dods src under our CVS
//
// Revision 1.6  2001/02/04 01:44:48  ndp
// Cleaned up javadoc errors
//
// Revision 1.5  1999/11/20 07:29:03  jimg
// Added new behavior for the Project property
//
// Revision 1.4  1999/09/24 23:17:58  ndp
// Added new constructors to all Exception classes
//
// Revision 1.3  1999/09/24 21:59:24  ndp
// Reorged Exceptions. Code compiles.
//
// Revision 1.2  1999/09/16 19:17:59  ndp
// *** empty log message ***
//
// Revision 1.1  1999/09/16 19:06:25  ndp
// Moved SomethingBadHappenedException to SBHException
//
// Revision 1.1  1999/09/16 18:56:06  ndp
//  Added SomethingBadHappenedException.java
//
// Revision 1.3  1999/08/20 22:59:43  jimg
// Changed the package declaration to dods.dap.Server so that it matches the
// directory name.
//

package dods.dap.Server;
import java.lang.String;
import dods.dap.DODSException;

/**
 * The Something Bad Happened (SBH) Exception.
 * This gets thrown in situations where something
 * pretty bad went down and we don't have a good
 * exception type to describe the problem, or
 * we don't really know what the hell is going on.
 * <p>
 * Yes, its the garbage dump of our exception
 * classes.
 *
 * @version $Revision: 1.1 $
 * @author ndp
 */
public class SBHException extends SDODSException {
  /**
   * Construct a <code>SBHException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public SBHException(String s) {
    super(DODSException.UNKNOWN_ERROR,"Ow! Something Bad Happened! All I know is: " + s);
  }


  /**
   * Construct a <code>SBHException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public SBHException(int err, String s) {
    super(err,s);
  }
}
