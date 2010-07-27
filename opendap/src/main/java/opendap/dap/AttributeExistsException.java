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


package opendap.dap;

/**
 * Thrown by <code>AttributeTable</code> when an attempt is made to create an
 * attribute that already exists. Creating an alias with the name of an
 * existing attribute will also trigger this exception.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see AttributeTable
 */
public class AttributeExistsException extends DASException {
    /**
     * Construct a <code>AttributeExistsException</code> with the specified
     * message.
     *
     * @param s the detail message.
     */
    public AttributeExistsException(String s) {
        super(opendap.dap.DAP2Exception.MALFORMED_EXPR, s);
    }


    /**
     * Construct a <code>AttributeExistsException</code> with the specified
     * message and OPeNDAP error code see (<code>DAP2Exception</code>).
     *
     * @param err the OPeNDAP error code.
     * @param s   the detail message.
     */
    public AttributeExistsException(int err, String s) {
        super(err, s);
    }
}


