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

import java.io.DataInputStream;
import java.io.PrintWriter;

/**
 * This class implements a simple list of OPeNDAP data
 * types. A list is a simple sequence of data items, without the
 * sophisticated subsetting and array indexing features of an Array.
 * <p/>
 * OPeNDAP does not support Lists of Lists. This restriction is enforced by the
 * DDS parser.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see BaseType
 * @see DVector
 */
public class DList extends DVector {
    /**
     * Constructs a new <code>DList</code>.
     */
    public DList() {
        super();
    }

    /**
     * Constructs a new <code>DList</code> with the given name.
     *
     * @param n the name of the variable.
     */
    public DList(String n) {
        super(n);
    }

    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    public String getTypeName() {
        return "List";
    }


}


