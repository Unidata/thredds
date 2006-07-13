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

package dods.dap;

/**
 * Thrown when an attempt is made to access a variable that does not exist.
 *
 * @version $Revision: 48 $
 * @author jehamby
 */
public class NoSuchVariableException extends DDSException {
  /**
   * Construct a <code>NoSuchVariableException</code> with the specified detail
   * message.
   *
   * @param s the detail message.
   */
  public NoSuchVariableException(String s) {
    super(DODSException.NO_SUCH_VARIABLE,s);
  }


  /**
   * Construct a <code>NoSuchVariableException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public NoSuchVariableException(int err, String s) {
    super(err,s);
  }
}
