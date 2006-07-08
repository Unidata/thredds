package dods.dap.Server;

/** Represents a top-level clause in the selection portion of a
 *  constraint expression (CE). <p>
 *
 *  A top-level clause is a boolean expression 
 *  preceded by "&" in the CE, such as "lat>10.0", or "function(var1,var2)". 
 *  The top-level clause may contain sub-clauses which can be evaluated 
 *  individually. 
 *
 *  The parser supports several kinds of top-level clause. These are described
 *  in the ClauseFactory interface.
 *
 * @see SubClause for more about sub-clauses.
 * @see CEEValuator for an explanation of how Clauses are evaluated on 
 *    data.
 * @see ClauseFactory
 *
 * @author joew */
public interface TopLevelClause 
    extends Clause {

    /** Returns the current value of the clause. The value of non-constant
     *  Clauses is undefined until the evaluate() method has been called.  */
    public boolean getValue();
    
    /** Evaluates the clause, first calling evaluate() on any sub-clauses it 
     *  contains. Implementations of this method  should flag the clause as 
     *  "defined" if the evaluation is successful.
     * @exception SDODSException Thrown if the evaluation fails for any reason.
     */
    public boolean evaluate() 
	throws SDODSException;
}
