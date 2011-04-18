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

import java.util.*;
import java.io.*;

import opendap.dap.parser.ExprParserConstants; // used only for toString()
import opendap.dap.Server.Operator;

/**
 * Represents a clause which compares subclauses, using one of the
 * relative operators supported by the Operator class.
 *
 * @author joew
 * @see Operator
 * @see ClauseFactory
 */
public class RelOpClause
        extends AbstractClause
        implements TopLevelClause {
    /**
     * Creates a new RelOpClause. If the lhs and all the elements of the rhs
     * are constant, the RelOpClause will be flagged as constant, and
     * evaluated immediately.
     *
     * @param operator The operator invoked by the clause
     * @param lhs      The left-hand side of the comparison.
     * @param rhs      A list of SubClauses representing the right-hand side of the
     *                 comparison.
     * @throws DAP2ServerSideException Thrown if the clause is constant, but
     *                        the attempt to evaluate it fails.
     */
    protected RelOpClause(int operator, SubClause lhs, List rhs) throws DAP2ServerSideException {

        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
        this.children = new ArrayList();
        children.add(lhs);
        children.addAll(rhs);
        this.constant = true;
        Iterator it = children.iterator();
        while (it.hasNext()) {
            SubClause current = (SubClause) it.next();
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
            throws DAP2ServerSideException {

        if (constant && defined) {
            return value;
        }

        if (rhs.size() == 1) {
            value = Operator.op(operator,
                    lhs.evaluate(),
                    ((SubClause) rhs.get(0)).evaluate());
        } else {
            value = false;
            Iterator it = rhs.iterator();
            while (it.hasNext() && !value) {
                if (Operator.op(operator, lhs.evaluate(), ((SubClause) it.next()).evaluate())) {
                    value = true;
                }
            }
        }
        defined = true;
        return value;
    }

    /**
     * Returns a SubClause representing the right-hand side of the
     * comparison.
     */
    public SubClause getLHS() {
        return lhs;
    }

    /**
     * Returns a list of SubClauses representing the right-hand side of the
     * comparison.
     */
    public List getRHS() {
        return rhs;
    }

    /**
     * Returns the type of comparison
     *
     * @see opendap.dap.parser.ExprParserConstants
     */
    public int getOperator() {
        return operator;
    }

    /**
     * Prints the original string representation of this clause.
     * For use in debugging.
     */
    public void printConstraint(PrintWriter os)
    {
        lhs.printConstraint(os);
        String op = ExprParserConstants.tokenImage[operator];
        op = op.substring(1, op.length() - 1);
        os.print(op);
        //os.print(ExprParserConstants.tokenImage[operator].substring(2, 3));
        if (rhs.size() == 1) {
            ((ValueClause)rhs.get(0)).printConstraint(os);
        } else {
            os.print("{");
            Iterator it = rhs.iterator();
	    boolean first = true;
            while (it.hasNext()) {
                if(!first) os.print(",");
	        ((ValueClause)it.next()).printConstraint(os);
		first = false;
            }
            os.print("}");
        }
    }

    protected boolean value;

    protected int operator;

    protected SubClause lhs;

    protected List rhs;
}
