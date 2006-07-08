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

// $Log: RegExpException.java,v $
// Revision 1.1  2005/12/16 22:06:59  caron
// dods src under our CVS
//
// Revision 1.5  2001/02/04 01:44:48  ndp
// Cleaned up javadoc errors
//
// Revision 1.4  1999/09/24 23:17:58  ndp
// Added new constructors to all Exception classes
//
// Revision 1.3  1999/09/24 21:59:23  ndp
// Reorged Exceptions. Code compiles.
//
// Revision 1.2  1999/09/16 23:25:50  ndp
// *** empty log message ***
//
// Revision 1.1  1999/09/16 22:04:29  ndp
// Moved RegExpSyntaxError.java to RegExpException.java
//
// Revision 1.2  1999/09/16 19:05:55  ndp
// Moved SomethingBadHappenedException to SBHException
//
// Revision 1.1  1999/09/16 18:28:55  ndp
// Added RegExpSyntaxError.java
//
// Revision 1.3  1999/08/20 22:59:43  jimg
// Changed the package declaration to dods.dap.Server so that it matches the
// directory name.
//

package dods.dap.Server;
import dods.dap.DODSException;

/**
 * Thrown by <code>Operator.op</code> when an attempt is made to parse a
 * improperly formed regular expression. Reular expressions should use
 * the same syntax as <i>grep</i>.
 * @version $Revision: 1.1 $
 * @author ndp
 * @see Operator#op(int,BaseType, BaseType)
 */
public class RegExpException extends SDODSException {
  /**
   * Construct a <code>RegExpException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public RegExpException(String s) {
    super(DODSException.MALFORMED_EXPR,"Syntax Error In Regular Expression: " + s);
  }


  /**
   * Construct a <code>RegExpException</code> with the specified
   * message and DODS error code see (<code>DODSException</code>).
   *
   * @param err the DODS error code.
   * @param s the detail message.
   */
  public RegExpException(int err, String s) {
    super(err,s);
  }
}
