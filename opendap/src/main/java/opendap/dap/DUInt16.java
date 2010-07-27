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
 * Holds a OPeNDAP <code>UInt16</code> value.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see BaseType
 */
public class DUInt16 extends DInt16 {
    /**
     * Constructs a new <code>DUInt16</code>.
     */
    public DUInt16() {
        super();
    }

    /**
     * Constructs a new <code>DUInt16</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public DUInt16(String n) {
        super(n);
    }

    /**
     * Constructs a new <code>UInt16PrimitiveVector</code>.
     *
     * @return a new <code>UInt16PrimitiveVector</code>.
     */
    public PrimitiveVector newPrimitiveVector() {
        return new UInt16PrimitiveVector(this);
    }

    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    public String getTypeName() {
        return "UInt16";
    }


    /**
     * Prints the value of the variable, with its declaration.  This
     * function is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     *
     * @param os           the <code>PrintWriter</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
     * @see BaseType#printVal(PrintWriter, String, boolean)
     */
    public void printVal(PrintWriter os, String space, boolean print_decl_p) {
        // to print properly, cast to long and convert unsigned to signed
        long tempVal = ((long) getValue()) & 0xFFFFL;
        if (print_decl_p) {
            printDecl(os, space, false);
            os.println(" = " + tempVal + ";");
        } else
            os.print(tempVal);
    }
}


