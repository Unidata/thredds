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




