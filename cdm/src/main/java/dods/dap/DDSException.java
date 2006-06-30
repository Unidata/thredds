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
 * DDS exception. This is the root of all the DDS exception classes.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see DODSException
 */
public class DDSException extends DODSException {
  /**
   * Construct a <code>DDSException</code> with the specified detail
   * message and DODS error code.
   *
   * @param s the detail message.
   */
  public DDSException(int error, String s) {
    super(error, s);
  }
}
