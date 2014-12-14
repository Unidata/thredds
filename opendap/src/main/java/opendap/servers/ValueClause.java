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

package opendap.servers;

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



    protected BaseType value;
    protected Clause parent;


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
    public void printConstraint(PrintWriter os)
    {
        if (constant) {
            value.printVal(os, "", false);
        } else {
            value.printConstraint(os);
        }
    }

}


