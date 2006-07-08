

/* $Id: BoolFunction.java,v 1.1 2005/12/16 22:06:59 caron Exp $
*
*/

package dods.dap.Server;

import java.util.List;

/** Represents a server-side function, which evaluates to a boolean value.
 *  Custom server-side functions which return boolean values
 *  should implement this interface.
 * @see BoolFunctionClause
 * @author joew */
public interface BoolFunction 
    extends ServerSideFunction {

    /** Evaluates the function using the argument list given.
     * @exception SDODSException Thrown if the function
     *  cannot evaluate successfully. The exact type of exception is up
     *  to the author of the server-side function.
     */
    public boolean evaluate(List args) 
	throws SDODSException;
}
