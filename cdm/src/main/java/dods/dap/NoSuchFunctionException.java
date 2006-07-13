/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, University of Rhode Island
// ALL RIGHTS RESERVED.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: James Gallagher <jgallagher@gso.uri.edu>
//
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

/** Thrown when an attempt is made to access a function that does not exist.
    @version $Revision: 48 $
    @author jhrg */

public class NoSuchFunctionException extends DDSException {

  /** Construct a <code>NoSuchFunctionException</code> with the specified
   * message.
   * @param s the detail message. */

   public NoSuchFunctionException(String s) {
       super(DODSException.MALFORMED_EXPR,s);
   }


  /**
   * Construct a <code>NoSuchFunctionException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public NoSuchFunctionException(int err, String s) {
    super(err,s);
  }
}
