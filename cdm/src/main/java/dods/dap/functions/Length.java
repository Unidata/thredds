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

package dods.dap.functions;
import dods.dap.*;

/** This class implements the length CE function which is used to return the
    length of List variables. Note that this is the prototypical CE function
    implementation. The function `length' is implemented in class
    `dods.dap.functions.Length' which has one method called main which takes
    an array of BaseType objects and returns its value in a BaseType object.
    @author jhrg
    @see CEEValuator
    @see FunctionClause 
    @version $Revision: 1.1 $
*/
public class Length {
    public final static BaseType main(BaseType args[])  {
	// There must be exactly one argument to this function and it must be
	// a List variable.
	return new DInt32("Length_is_unimplemented");
    }
}
