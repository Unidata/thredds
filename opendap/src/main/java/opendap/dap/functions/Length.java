/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
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


package opendap.dap.functions;

import opendap.dap.*;

/**
 * This class implements the length CE function which is used to return the
 * length of List variables. Note that this is the prototypical CE function
 * implementation. The function `length' is implemented in class
 * `opendap.dap.functions.Length' which has one method called main which takes
 * an array of BaseType objects and returns its value in a BaseType object.
 *
 * @author jhrg
 * @version $Revision: 15901 $
 */
public class Length {
    public static BaseType main(BaseType args[]) {
        // There must be exactly one argument to this function and it must be
        // a List variable.
        return new DInt32("Length_is_unimplemented");
    }
}


