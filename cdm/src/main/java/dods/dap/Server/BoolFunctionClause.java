package dods.dap.Server;

import java.util.*;

/** Represents a clause which invokes a function that returns a boolean value. 
 * @see ClauseFactory
 * @author joew */
public class BoolFunctionClause
    extends AbstractClause
    implements TopLevelClause {

    /** Creates a new BoolFunctionClause.
     * @param function The function invoked by the clause
     * @param children A list of SubClauses, to be given as arguments 
     * to the function. If all the arguments are constant, the function
     * clause will be flagged as constant, and evaluated immediatelyx.
     * @exception SDODSException Thrown if either 1) the function does not 
     * accept the arguments given, or 2) the 
     * clause is constant, and the attempt to evaluate it fails. 
     */
    protected BoolFunctionClause(BoolFunction function,
				 List children) 
	throws SDODSException {

	function.checkArgs(children);
	this.function = function;
	this.children = children;
	this.constant = true;
	Iterator it = children.iterator();
	while (it.hasNext()) {
	    SubClause current = (SubClause)it.next();
	    current.setParent(this);
	    if (!current.isConstant()) {
		constant = false;
	    }
	}
	if (constant) {
	    evaluate();
	}
    }
    
    public boolean getValue() {
	return value;
    }
    
    public boolean evaluate() 
	throws SDODSException {

	if (!constant || !defined) {
	    defined = true;
	    value = function.evaluate(children);
	}
	return value;
    }

    /** Returns the server-side function invoked by this clause */
    public BoolFunction getFunction() {
	return function;
    }

    /** Prints the original string representation of this clause.
     *  For use in debugging.
     */
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append(function.getName());
	buf.append("(");
	Iterator it = children.iterator();
	if (it.hasNext()) {
	    buf.append(it.next().toString());
	} 
	while (it.hasNext()) {
	    buf.append(",");
	    buf.append(it.next().toString());
	}
	buf.append(")");
	return buf.toString();
    }

    protected BoolFunction function;

    protected boolean value;
}
