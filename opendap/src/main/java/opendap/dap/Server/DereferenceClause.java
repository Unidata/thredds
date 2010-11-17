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

import java.util.*;

import opendap.dap.BaseType;

/**
 * Represents a sub-clause that is a URL reference to remote data.
 * This feature is not yet supported in Java. Thus this class
 * throws an exception in its constructor.
 *
 * @author joew
 * @see ClauseFactory
 */
public class DereferenceClause
        extends AbstractClause
        implements SubClause {

    /**
     * Creates a new DereferenceClause
     */
    protected DereferenceClause(String url)
            throws DAP2ServerSideException {
        this.url = url;
        this.constant = true;
        this.defined = true;
        this.value = retrieve(url);
        this.children = new ArrayList();
    }

    public BaseType getValue() {
        return value;
    }

    public BaseType evaluate() {
        return value;
    }

    public Clause getParent() {
        return parent;
    }

    public void setParent(Clause parent) {
        this.parent = parent;
    }

    public String getURL() {
        return url;
    }

    protected BaseType retrieve(String url)
            throws DAP2ServerSideException {

        throw new DAP2ServerSideException(opendap.dap.DAP2Exception.UNKNOWN_ERROR, "dereferencing not supported");
    }

    public String toString() {
        return "*\"" + url + "\"";
    }

    protected String url;
    protected Clause parent;
    protected BaseType value;
}




