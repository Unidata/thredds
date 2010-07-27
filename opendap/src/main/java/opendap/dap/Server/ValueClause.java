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
import java.io.*;

import opendap.dap.BaseType;

/**
 * Represents a clause containing a simple value. If the value is
 * an constant value such as "2.0", the clause's isConstant() method
 * will return true; if it is a variable of the dataset, isConstant() will
 * return false.
 *
 * @author Joe Wielgosz (joew@cola.iges.org)
 * @see ClauseFactory
 */
public class ValueClause
        extends AbstractClause
        implements SubClause {

    /**
     * Creates a new ValueClause.
     *
     * @param value    The BaseType represented by this clause. This can
     *                 be either a BaseType taken from the DDS of a dataset, or a BaseType
     *                 object created to hold a constant value.
     * @param constant Should be set to false if the value parameter is
     *                 from the DDS of a dataset, and true if the value parameter is a
     *                 constant value.
     */
    protected ValueClause(BaseType value,
                          boolean constant) {
        this.value = value;
        this.constant = constant;
        this.defined = constant;
        this.children = new ArrayList();
    }

    /**
     * Returns the BaseType represented by this clause.
     */
    public BaseType getValue() {
        return value;
    }

    /**
     * Returns the BaseType represented by this clause. Equivalent to
     * getValue(), except that
     * calling this method flags this clause as "defined".
     *
     * @throws DAP2ServerSideException Not thrown by this type of clause.
     */
    public BaseType evaluate() {
        defined = true;
        return value;
    }

    public Clause getParent() {
        return parent;
    }

    public void setParent(Clause parent) {
        this.parent = parent;
    }

    /**
     * Prints the original string representation of this clause.
     * For use in debugging.
     */
    public String toString() {
        if (constant) {
            StringWriter w = new StringWriter();
            value.printVal(new PrintWriter(w), "", false);
            return w.toString();
        } else {
            return value.getName();
        }
    }

    protected BaseType value;
    protected Clause parent;

}


