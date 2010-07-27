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
 * Represents common interface of all server-side functions (SSF).
 * Implementing this interface is not sufficient to create a working
 * SSF. SSF's must implement (at least) one of the subinterfaces
 * BoolFunction and BTFunction.
 *
 * @author joew
 */
public interface ServerSideFunction {

    /**
     * Returns the name of the server-side function, as it will appear in
     * constraint expressions. This must be a valid OPeNDAP identifier.
     * All functions must have distinct names.
     */
    public String getName();

    /**
     * Checks that the arguments given are acceptable arguments for this
     * function. This method should only use those attributes of a SubClause
     * which do not change over its lifetime - whether it is constant,
     * what class of SubClause it is, what class of BaseType it returns, etc.
     * Thus, the method should not look at the actual value of an argument
     * unless the argument is flagged as constant.
     *
     * @param args A list of SubClauses that the caller is considering passing
     *             to the evaluate() method of the function.
     * @throws InvalidParameterException Thrown if the function will not
     *                                   evaluate successfully using these arguments.
     */
    public void checkArgs(List args)
            throws InvalidParameterException;
}


