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
 * DDS exception. This is the root of all the DDS exception classes.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see DAP2Exception
 */
public class DDSException extends DAP2Exception {
    /**
     * Construct a <code>DDSException</code> with the specified detail
     * message and OPeNDAP DAP2 error code.
     *
     * @param error The DAP2 error code.
     * @param s the detail message.
     * @see DAP2Exception
     */
    public DDSException(int error, String s) {
        super(error, s);
    }
}


