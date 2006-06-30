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
 * Thrown by <code>AttributeTable</code> when an attempt is made to alias to
 * a non-existent attribute.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see AttributeTable#addAlias(String, String)
 */
public class NoSuchAttributeException extends DASException {
  /**
   * Construct a <code>NoSuchAttributeException</code> with the specified
   * message.
   *
   * @param s the detail message.
   */
  public NoSuchAttributeException(String s) {
    super(DODSException.MALFORMED_EXPR, s);
  }


  /**
   * Construct a <code>NoSuchAttributeException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public NoSuchAttributeException(int err, String s) {
    super(err,s);
  }
}
