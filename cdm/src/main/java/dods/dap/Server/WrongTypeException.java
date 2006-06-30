/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, Univ. of Rhode Island
// ALL RIGHTS RESERVED.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: James Gallagher <jgallagher@gso.uri.edu>
//
/////////////////////////////////////////////////////////////////////////////

package dods.dap.Server;
import java.lang.String;
import dods.dap.DODSException;

/** Report a type-mismatch problem in the constraint expression. Examples are
 * using relational operators on arrays, using the dot notation on variables
 * that are not aggregate types.
 * @author jhrg
 * @version $Revision: 1.1 $
 * @see SDODSException
 * @see DODSException */

public class WrongTypeException extends SDODSException {
    /** Construct a <code>WrongTypeException</code> with the specified
     * detail message.
     * @param s the detail message. */
    public WrongTypeException(String s) {
	super(MALFORMED_EXPR, s);
    }
} 
