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
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(lhs.toString());

        String op = ExprParserConstants.tokenImage[operator];
        op = op.substring(1, op.length() - 1);
        buf.append(op);
        //buf.append(ExprParserConstants.tokenImage[operator].substring(2, 3));

        if (rhs.size() == 1) {
            buf.append(rhs.get(0).toString());
        } else {
            buf.append("{");
            Iterator it = rhs.iterator();
            buf.append(it.next());
            while (it.hasNext()) {
                buf.append(",");
                buf.append(it.next().toString());
            }
            buf.append("}");
        }
        return buf.toString();
    }

    protected boolean value;

    protected int operator;

    protected SubClause lhs;

    protected List rhs;
}


