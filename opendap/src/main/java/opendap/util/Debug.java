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

import java.util.TreeMap;

/**
 * A minimal implementation of a globally-accessible set of Debug flags.
 */

public class Debug {
    static private TreeMap map = new TreeMap();
    static private boolean debug = false, changed = true;

    static public boolean isSet(String flagName) {
        Object val;
        if (null == (val = map.get(flagName))) {
            if (debug) System.out.println("Debug.isSet new " + flagName);
            map.put(flagName, new Boolean(false));
            changed = true;
            return false;
        }

        return ((Boolean) val).booleanValue();
    }

    static public void set(String flagName, boolean value) {
        Object val;
        if (null == (val = map.get(flagName))) {
            changed = true;
        }
        map.put(flagName, new Boolean(value));
        if (debug) System.out.println("  Debug.set " + flagName + " " + value);
    }

    static public void clear() {
        map = new TreeMap();
    }

    static public java.util.Set keySet() {
        return map.keySet();
    }
}

/**
 * $Log: Debug.java,v $
 * Revision 1.1  2003/08/12 23:51:27  ndp
 * Mass check in to begin Java-OPeNDAP development work
 *
 * Revision 1.2  2002/09/13 21:12:08  caron
 * add keySet()
 *
 * Revision 1.1  2001/10/24 22:51:42  ndp
 * *** empty log message ***
 *
 * Revision 1.1.1.1  2001/09/26 15:36:47  caron
 * checkin beta1
 *
 */


