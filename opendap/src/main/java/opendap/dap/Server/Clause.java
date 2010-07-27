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

import java.util.List;

/**
 * Represents the common interface of the two types of clause used by the
 * constraint expression (CE) parser: TopLevelClause and SubClause.
 * See these interfaces for more about CE parsing and evaluation.
 *
 * @author joew
 */
public interface Clause {

    /**
     * Returns an ordered list of this clause's sub-clauses.  If the
     * clause has no sub-clauses, an empty list will be returned.
     */
    public List getChildren();

    /**
     * A clause is considered "constant" iff it and its subclauses do not
     * refer to data values from the dataset being constrained.  A
     * constant clause is defined as soon as it is created, and is
     * guaranteed not to change its value during its lifetime.
     */
    public boolean isConstant();

    /**
     * Returns whether or not the clause has a defined value. Non-constant
     * clauses do not have a defined value until they are evaluated for the
     * first time. Methods for evaluating are found in the TopLevelClause
     * and SubClause interfaces.
     */
    public boolean isDefined();
}


