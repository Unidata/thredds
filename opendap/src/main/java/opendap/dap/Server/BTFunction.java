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

import java.util.List;

import opendap.dap.BaseType;
import opendap.dap.InvalidParameterException;

/**
 * Represents a server-side function, which evaluates to a BaseType.
 * Custom server-side functions which return non-boolean values should
 * implement this interface.  For an efficient implementation, it is
 * suggested, when possible, to use the same BaseType for the getType()
 * method and for each successive invocation of evaluate(), changing only
 * the BaseType's value. This avoids creation of large numbers of
 * BaseTypes during a data request.
 *
 * @author joew
 * @see BTFunctionClause
 */
public interface BTFunction
        extends ServerSideFunction {

    /**
     * A given function must always evaluate to the same class
     * of BaseType. Only the value held by the BaseType may change.
     * This method can be used to discover the BaseType class of a
     * function without actually evaluating it.
     */
    public BaseType getReturnType(List args)
            throws InvalidParameterException;

    /**
     * Evaluates the function using the argument list given.
     *
     * @throws DAP2ServerSideException Thrown if the function
     *                        cannot evaluate successfully. The exact type of exception is up
     *                        to the author of the server-side function.
     */
    public BaseType evaluate(List args)
            throws DAP2ServerSideException;
}


