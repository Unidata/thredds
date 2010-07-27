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

/**
 * Used to indicate that one of the passed parameters to a method
 * is either the wrong type, is missing, or it's value is
 * unacceptable.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class InvalidParameterException extends DAP2ServerSideException {
    /**
     * Construct a <code>InvalidParameterException</code> with the specified
     * detail message.
     *
     * @param s the detail message.
     */
    public InvalidParameterException(String s) {
        super(opendap.dap.DAP2Exception.MALFORMED_EXPR, "Invalid Parameter Exception: " + s);
    }


    /**
     * Construct a <code>InvalidParameterException</code> with the specified
     * message and OPeNDAP error code (see <code>DAP2Exception</code>).
     *
     * @param err the OPeNDAP error code.
     * @param s   the detail message.
     */
    public InvalidParameterException(int err, String s) {
        super(err, s);
    }
}


