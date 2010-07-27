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



package opendap.util;

/**
 * Exception throwable by the Getopts class
 */
public class InvalidSwitch extends Throwable {
/**
 *   This is the specific exception that is thrown when an invalid
 *   switch or another problem occurred in the Getopts class.
 */
    /**
     * Used when no notification string other than the
     * standard one will be thrown with the exception.
     */
    public InvalidSwitch() {
        super();
    }

    /**
     * Used when passing the string that will be thrown.
     *
     * @param    s    the error string that is notified
     */
    public InvalidSwitch(String s) {
        super(s);
    }
}


