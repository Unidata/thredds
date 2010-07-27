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

/**
 * Provides default implementations for the Clause interface methods.
 * This eliminates redundant code and provides a starting point
 * for new implementations of Clause. <p>
 * <p/>
 * Note that every Clause object is expected to implement either
 * SubClause or TopLevelClause. No default implementations are provided
 * for the methods of these subinterfaces <p>
 * <p/>
 * Also note that it is not <i>necessary</i> to use this class to
 * create your own implementations, it's just a convenience.<p>
 * <p/>
 * The class has no abstract methods, but is declared abstract because
 * it should not be directly instantiated.
 *
 * @author joew
 */
public abstract class AbstractClause implements Clause {

    public List getChildren() {
        return children;
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isDefined() {
        return defined;
    }

    /**
     * Value to be returned by isConstant(). Should not change for
     * the lifetime of the object.
     */
    protected boolean constant;

    /**
     * Value to be returned by isDefined(). May change during the
     * lifetime of the object.
     */
    protected boolean defined;

    /**
     * A list of SubClause objects representing
     * the sub-clauses of this clause. Use caution when modifying
     * this list other than at the point of creation, since methods
     * such as evaluate() depend on it.
     */
    protected List children;
}


