package dods.dap.Server;

import java.util.List;

/** Represents common interface of all server-side functions (SSF).
 *  Implementing this interface is not sufficient to create a working
 *  SSF. SSF's must implement (at least) one of the subinterfaces
 *  BoolFunction and BTFunction.
 * @author joew */
public interface ServerSideFunction {

    /** Returns the name of the server-side function, as it will appear in
     *  constraint expressions. This must be a valid DODS identifier.
     *  All functions must have distinct names. 
     */
    public String getName();

    /** Checks that the arguments given are acceptable arguments for this 
     * function. This method should only use those attributes of a SubClause
     * which do not change over its lifetime - whether it is constant,
     * what class of SubClause it is, what class of BaseType it returns, etc.
     * Thus, the method should not look at the actual value of an argument 
     * unless the argument is flagged as constant.
     * 
     * @param args A list of SubClauses that the caller is considering passing
     *             to the evaluate() method of the function.
     * @exception InvalidParameterException Thrown if the function will not
     * evaluate successfully using these arguments.
     * @return The function should return normally if the arguments appear
     * acceptable, and throw an exception describing the problem otherwise.
     */
    public void checkArgs(List args)
	throws InvalidParameterException;
}
