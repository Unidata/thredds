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
 * Thrown by <code>Attribute</code> when a bad value
 * (not one of the supported types) is stored in an Attribute.
 *
 * @version $Revision: 48 $
 * @author jehamby
 * @see Attribute
 */
public class AttributeBadValueException extends DASException {

  /**
   * Construct a <code>AttributeBadValueException</code> with the specified
   * message.
   *
   * @param s the detail message.
   */
  public AttributeBadValueException(String s) {
    super(DODSException.MALFORMED_EXPR,s);
  }


  /**
   * Construct a <code>AttributeBadValueException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public AttributeBadValueException(int err, String s) {
    super(err,s);
  }
}
