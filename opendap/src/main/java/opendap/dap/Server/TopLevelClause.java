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


