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

/**
 * Represents a top-level clause in the selection portion of a
 * constraint expression (CE). <p>
 * <p/>
 * A top-level clause is a boolean expression
 * preceded by "&" in the CE, such as "lat>10.0", or "function(var1,var2)".
 * The top-level clause may contain sub-clauses which can be evaluated
 * individually.
 * <p/>
 * The parser supports several kinds of top-level clause. These are described
 * in the ClauseFactory interface.
 * <p/>
 * <p>See <code>SubClause</code> for more about sub-clauses.
 * <p> See <code>CEEValuator</code> for an explanation of how Clauses
 * are evaluated on data.
 *
 * @author joew
 * @see SubClause
 * @see CEEvaluator
 * @see ClauseFactory
 */
public interface TopLevelClause
        extends Clause {

    /**
     * Returns the current value of the clause. The value of non-constant
     * Clauses is undefined until the evaluate() method has been called.
     */
    public boolean getValue();

    /**
     * Evaluates the clause, first calling evaluate() on any sub-clauses it
     * contains. Implementations of this method  should flag the clause as
     * "defined" if the evaluation is successful.
     *
     * @throws DAP2ServerSideException Thrown if the evaluation fails for any reason.
     */
    public boolean evaluate()
            throws DAP2ServerSideException;
}


