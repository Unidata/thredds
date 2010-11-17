/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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


