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
 * DAP2 Exception for use by a server.
 * This is the root of all the OPeNDAP Server exception classes.
 *
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class DAP2ServerSideException extends opendap.dap.DAP2Exception {
    /**
     * Construct a <code>DAP2ServerSideException</code> with the specified detail
     * message.
     *
     * @param err The error number. See opendap.dap.DAP2Exception for the values
     * and their meaning.
     * @param s the detail message.
     * @see opendap.dap.DAP2Exception
     */
    public DAP2ServerSideException(int err, String s) {
        super(err, s);
    }
}


