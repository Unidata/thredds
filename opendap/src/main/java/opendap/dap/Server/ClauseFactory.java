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


