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
 * Represents a server-side function, which evaluates to a boolean value.
 * Custom server-side functions which return boolean values
 * should implement this interface.
 *
 * @author joew
 * @see BoolFunctionClause
 */
public interface BoolFunction
        extends ServerSideFunction {

    /**
     * Evaluates the function using the argument list given.
     *
     * @throws DAP2ServerSideException Thrown if the function
     *                        cannot evaluate successfully. The exact type of exception is up
     *                        to the author of the server-side function.
     */
    public boolean evaluate(List args)
            throws DAP2ServerSideException;
}


