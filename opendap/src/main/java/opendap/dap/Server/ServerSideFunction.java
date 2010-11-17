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


