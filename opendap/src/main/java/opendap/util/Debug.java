/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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


