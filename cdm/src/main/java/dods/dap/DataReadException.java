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
 * Thrown when DODS encounters an exception while reading from a data set.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
 */
public class DataReadException extends DDSException {
  /**
   * Construct a <code>DataReadException</code> with the specified detail
   * message.
   *
   * @param s the detail message.
   */
  public DataReadException(String s) {
    super(DODSException.CANNOT_READ_FILE,s);
  }


  /**
   * Construct a <code>DataReadException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public DataReadException(int err, String s) {
    super(err,s);
  }
}
