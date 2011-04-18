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

package opendap.Server;

import java.util.*;
import java.io.*;

import opendap.dap.BaseType;

/**
 * Represents a clause which invokes a function that returns a BaseType.
 *
 * @author joew
 * @see ClauseFactory
 */
public class BTFunctionClause
        extends AbstractClause
        implements SubClause {

    /**
     * Creates a new BTFunctionClause.
     *
     * @param function The function invoked by the clause
     * @param children A list of SubClauses, to be given as arguments
     *                 to the function. If all the arguments are constant, the function
     *                 clause will be flagged as constant, and evaluated immediately.
     * @throws DAP2ServerSideException Thrown if either 1) the function does not
     *                        accept the arguments given, or 2) the
     *                        clause is constant, and the attempt to evaluate it fails.
     */
    protected BTFunctionClause(BTFunction function,
                               List children)
            throws DAP2ServerSideException {

        function.checkArgs(children);
        this.function = function;
        this.children = children;
        this.constant = true;
        Iterator it = children.iterator();
        while (it.hasNext()) {
            SubClause current = (SubClause) it.next();
            current.setParent(this);
            if (!current.isConstant()) {
                constant = false;
            }
        }
        value = function.getReturnType(children);
        if (constant) {
            evaluate();
        }
    }

    public Clause getParent() {
        return parent;
    }

    public BaseType getValue() {
        return value;
    }

    public BaseType evaluate()
            throws DAP2ServerSideException {

        if (!constant || !defined) {
            value = function.evaluate(children);
            defined = true;
        }
        return value;
    }

    public void setParent(Clause parent) {
        this.parent = parent;
    }

    /**
     * Returns the server-side function invoked by this clause
     */
    public BTFunction getFunction() {
        return function;
    }

    /**
     * Prints the original string representation of this clause.
     * For use in debugging.
     */
    public void printConstraint(PrintWriter os)
    {
        os.print(function.getName()+"(");
        Iterator it = children.iterator();
	boolean first = true;
        while (it.hasNext()) {
	    ValueClause vc = (ValueClause)it.next();
            if(!first) os.print(",");
	    vc.printConstraint(os);
	    first = false;
        }
        os.print(")");
    }

    protected Clause parent;

    protected BTFunction function;

    protected BaseType value;

}


