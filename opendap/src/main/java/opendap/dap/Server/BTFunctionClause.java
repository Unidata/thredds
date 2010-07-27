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

import java.util.*;

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

    protected Clause parent;

    protected BTFunction function;

    protected BaseType value;

}


