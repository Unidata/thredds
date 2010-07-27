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



package opendap.servers.ascii;

import java.io.*;

import opendap.dap.*;

/**
 */
public class asciiI32 extends DInt32 implements toASCII {

    private static boolean _Debug = false;

    /**
     * Constructs a new <code>asciiI32</code>.
     */
    public asciiI32() {
        this(null);
    }

    /**
     * Constructs a new <code>asciiI32</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public asciiI32(String n) {
        super(n);
    }


    /**
     * Returns a string representation of the variables value. This
     * is really foreshadowing functionality for Server types, but
     * as it may come in useful for clients it is added here. Simple
     * types (example: DFloat32) will return a single value. DConstuctor
     * and DVector types will be flattened. DStrings and DURL's will
     * have double quotes around them.
     *
     * @param addName is a flag indicating if the variable name should
     *                appear at the begining of the returned string.
     */
    public void toASCII(PrintWriter pw,
                        boolean addName,
                        String rootName,
                        boolean newLine) {

        rootName = toASCIIAddRootName(pw, addName, rootName);

        if (addName)
            pw.print(", ");

        pw.print((new Long(getValue())).toString());

        if (newLine)
            pw.print("\n");

    }


    public String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName) {

        if (addName) {
            rootName = toASCIIFlatName(rootName);
            pw.print(rootName);
        }
        return (rootName);

    }

    public String toASCIIFlatName(String rootName) {
        String s;
        if (rootName != null) {
            s = rootName + "." + getName();
        } else {
            s = getName();
        }
        return (s);
    }


}


