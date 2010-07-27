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

import java.io.*;

/**
 * A minimal implementation of a logging facility.
 */

public class Log {

    static private PrintStream logger = null;
    static private ByteArrayOutputStream buff = null;

    static public void println(String s) {
        if (logger != null)
            logger.println(s);
    }

    static public void printDODSException(opendap.dap.DAP2Exception de) {
        if (logger != null) {
            de.print(logger);
            de.printStackTrace(logger);
        }
    }

    static public void printThrowable(Throwable t) {
        if (logger != null) {
            logger.println(t.getMessage());
            t.printStackTrace(logger);
        }
    }

    static public void reset() {
        buff = new ByteArrayOutputStream();
        logger = new PrintStream(buff);
    }

    static public boolean isOn() {
        return (logger != null);
    }

    static public void close() {
        logger = null;
        buff = null;
    }

    static public String getContents() {
        if (buff == null)
            return "null";
        else {
            logger.flush();
            return buff.toString();
        }
    }

}

/**
 * $Log: Log.java,v $
 * Revision 1.1  2003/08/12 23:51:27  ndp
 * Mass check in to begin Java-OPeNDAP development work
 *
 * Revision 1.1  2002/09/24 18:32:35  caron
 * add Log.java
 *
 *
 */


