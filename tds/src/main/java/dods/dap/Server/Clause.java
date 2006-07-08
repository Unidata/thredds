package dods.dap.Server;

import java.util.List;

/** Represents the common interface of the two types of clause used by the
 *  constraint expression (CE) parser: TopLevelClause and SubClause.
 *  See these interfaces for more about CE parsing and evaluation.
 * 
 * @author joew */
public interface Clause {

    /** Returns an ordered list of this clause's sub-clauses.  If the
     *  clause has no sub-clauses, an empty list will be returned.  */
    public List getChildren();

    /** A clause is considered "constant" iff it and its subclauses do not
     *  refer to data values from the dataset being constrained.  A
     *  constant clause is defined as soon as it is created, and is
     *  guaranteed not to change its value during its lifetime.  */
    public boolean isConstant();
    
    /** Returns whether or not the clause has a defined value. Non-constant
     *  clauses do not have a defined value until they are evaluated for the
     *  first time. Methods for evaluating are found in the TopLevelClause
     *  and SubClause interfaces. */
    public boolean isDefined();
}
