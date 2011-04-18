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

import opendap.dap.BaseType;
import opendap.dap.NoSuchFunctionException;

import java.util.List;

/**
 * Represents a source of Clause objects for the constraint expression
 * parser.  By inheriting from this class and overriding the "newX" methods,
 * you can create a factory that provides your own Clause objects instead
 * of the default ones. This custom factory can be given to the parser
 * via the CEEvaluator interface.
 *
 * @see CEEvaluator
 */
public class ClauseFactory {

    /**
     * Creates a new clause factory with a blank function library. This
     * constructor is sufficient for servers with no server side functions.
     *
     * @see FunctionLibrary
     */
    public ClauseFactory() {
        this.functionLibrary = new FunctionLibrary();
    }

    /**
     * Creates a clause factory which uses the specified function library.
     * This constructor allows you to parse CE's using a customized function
     * library.
     *
     * @param functionLibrary The function library that will be used
     *                        when creating clauses that invoke server-side functions.
     */
    public ClauseFactory(FunctionLibrary functionLibrary) {
        this.functionLibrary = functionLibrary;
    }

    /**
     * Generates a clause which which compares subclauses, using one of the
     * relative operators supported by the Operator class.
     */
    public TopLevelClause newRelOpClause(int operator,
                                         SubClause lhs,
                                         List rhs)
            throws DAP2ServerSideException {

        return new RelOpClause(operator,
                lhs,
                rhs);
    }

    /**
     * Generates a clause which invokes a function that returns a
     * boolean value.
     *
     * @see BoolFunctionClause
     */
    public TopLevelClause newBoolFunctionClause(String functionName,
                                                List children)
            throws DAP2ServerSideException,
            NoSuchFunctionException {

        BoolFunction function =
                functionLibrary.getBoolFunction(functionName);
        if (function == null) {
            if (functionLibrary.getBTFunction(functionName) != null) {
                throw new NoSuchFunctionException
                        ("The function " + functionName +
                                "() does not return a " +
                                "boolean value, and must be used in a comparison or " +
                                "as an argument to another function.");
            } else {
                throw new NoSuchFunctionException
                        ("This server does not support a " +
                                functionName + "() function");
            }
        }
        return new BoolFunctionClause(function,
                children);
    }

    /**
     * Generates a clause representing a simple value,
     * such as "var1" or "19".
     *
     * @see ValueClause
     */
    public SubClause newValueClause(BaseType value,
                                    boolean constant)
            throws DAP2ServerSideException {

        return new ValueClause(value,
                constant);
    }

    /**
     * Generates a clause which invokes a function that returns a
     * BaseType.
     *
     * @see BTFunctionClause
     */
    public SubClause newBTFunctionClause(String functionName,
                                         List children)
            throws DAP2ServerSideException,
            NoSuchFunctionException {

        BTFunction function =
                functionLibrary.getBTFunction(functionName);
        if (function == null) {
            if (functionLibrary.getBoolFunction(functionName) != null) {
                throw new NoSuchFunctionException
                        ("The function " + functionName +
                                "() cannot be used as a " +
                                "sub-expression in a constraint clause");
            } else {
                throw new NoSuchFunctionException
                        ("This server does not support a " +
                                functionName + "() function");
            }
        }
        return new BTFunctionClause(function,
                children);
    }

    /**
     * Generates a clause representing a remote value, referenced by a URL.
     * Note that dereferencing is not currently supported, and the default
     * implementation of this clause type throws an exception when it is
     * evaluated.
     *
     * @see DereferenceClause
     */
    public SubClause newDereferenceClause(String url)
            throws DAP2ServerSideException {

        return new DereferenceClause(url);
    }

    protected FunctionLibrary functionLibrary;

}


