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

import java.io.*;

/**
 * A vector of unsigned ints.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see PrimitiveVector
 */
public class UInt16PrimitiveVector extends Int16PrimitiveVector {
    /**
     * Constructs a new <code>UInt16PrimitiveVector</code>.
     */
    public UInt16PrimitiveVector(BaseType var) {
        super(var);
    }

    /**
     * Prints the value of all variables in this vector.  This
     * method is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     *
     * @param os    the <code>PrintWriter</code> on which to print the value.
     * @param space this value is passed to the <code>printDecl</code> method,
     *              and controls the leading spaces of the output.
     * @see BaseType#printVal(PrintWriter, String, boolean)
     */
    public void printVal(PrintWriter os, String space) {
        int len = getLength();
        for (int i = 0; i < len - 1; i++) {
            // to print properly, cast to long and convert to unsigned
            os.print(((long) getValue(i)) & 0xFFFFL);
            os.print(", ");
        }
        // print last value, if any, without trailing comma
        if (len > 0)
            os.print(((long) getValue(len - 1)) & 0xFFFFL);
    }

    /**
     * Prints the value of a single variable in this vector.
     * method is used by <code>DArray</code>'s <code>printVal</code> method.
     *
     * @param os    the <code>PrintWriter</code> on which to print the value.
     * @param index the index of the variable to print.
     * @see DArray#printVal(PrintWriter, String, boolean)
     */
    public void printSingleVal(PrintWriter os, int index) {
        // to print properly, cast to long and convert to unsigned
        os.print(((long) getValue(index)) & 0xFFFFL);
    }
}


