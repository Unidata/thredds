/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap.Server;

import opendap.dap.BaseType;

/**
 * Represents a sub-clause of the selection portion of a constraint
 * expression. A sub-clause is any part of a constraint that
 * can be evaluated to a BaseType value. For instance, the constraint
 * "var1>=function(var2,var3)" would have sub clauses "var1" and
 * "function(var2,var3)". The latter would in turn have the sub-clauses
 * "var2" and "var3".  <p>
 * <p/>
 * A given instance of SubClause may change the value it returns when
 * evaluated multiple times, but should not change the class of BaseType
 * it returns. This allows function and operator clauses to do type-checking
 * using the getValue() method.
 * <p/>
 * The parser supports several kinds of sub-clause. These are described
 * in the ClauseFactory interface.
 * <p/>
 * <p>See <code>TopLevelClause</code> for more about the parsing of clauses.
 * <p> See <code>CEEValuator</code> for an explanation of how Clauses
 * are evaluated on data.
 *
 * @author joew
 * @see TopLevelClause
 * @see CEEvaluator
 * @see ClauseFactory
 */
public interface SubClause extends Clause {

    /**
     * Returns the Clause which contains this subclause. The clause returned
     * may be a TopLevelClause or another SubClause.
     */
    public Clause getParent();

    /**
     * Returns a BaseType containing the current value of the sub-clause.
     * Sub-clauses that are not constant have an undefined value until the
     * evaluate() method has been called. However, in such circumstances
     * this method is still useful, as it indicates which class of
     * BaseType the sub-clause will evaluate to. Implementations of this
     * method should never return null.
     */
    public BaseType getValue();

    /**
     * Evaluates the clause, first calling evaluate() on any sub-clauses it
     * contains. Implementations of this method  should flag the clause as
     * "defined" if the evaluation is successful.
     *
     * @throws DAP2ServerSideException Thrown if the evaluation fails for any reason.
     */
    public BaseType evaluate() throws DAP2ServerSideException;

    /**
     * Sets the parent of this subclause. Used during parsing.
     */
    public void setParent(Clause parent);
}


