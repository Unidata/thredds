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
 * Thrown by <code>BaseType</code> when the <code>checkSemantics</code>
 * method is called and the variable is not valid (the name is null or some
 * other semantic violation).
 *
 * @version $Revision: 48 $
 * @author jehamby
 * @see BaseType
 */
public class BadSemanticsException extends DDSException {
  /**
   * Construct a <code>BadSemanticsException</code> with the specified detail
   * message.
   *
   * @param s the detail message.
   */
  public BadSemanticsException(String s) {
    super(DODSException.MALFORMED_EXPR,s);
  }


  /**
   * Construct a <code>BadSemanticsException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public BadSemanticsException(int err, String s) {
    super(err,s);
  }
}
