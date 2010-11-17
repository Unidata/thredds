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


