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


package opendap.servers.ascii;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;

/**
 */
public class asciiSeq extends DSequence implements toASCII {

    private static boolean _Debug = false;

    /**
     * Constructs a new <code>asciiSeq</code>.
     */
    public asciiSeq() {
        this(null);
    }

    /**
     * Constructs a new <code>asciiSeq</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public asciiSeq(String n) {
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

        if (_Debug) System.out.println("asciiSeq.toASCII(" + addName + ",'" + rootName + "')  getName(): " + getName());
        //System.out.println("this: " + this + " Has "+allValues.size() + " elements.");

        if (rootName != null)
            rootName += "." + getName();
        else
            rootName = getName();

        pw.print(toASCIIFlatName(rootName));

/*
        for(Enumeration e1 = allValues.elements(); e1.hasMoreElements(); ) {
            // get next instance vector
            Vector v = (Vector)e1.nextElement();
            for(Enumeration e2 = v.elements(); e2.hasMoreElements(); ) {
                // get next instance variable
                BaseType bt = (BaseType)e2.nextElement();

        pw.print(bt.toASCIIFlatName(rootName)+",");
            }
        break;
        }
*/

        pw.println("");

        int i = 0;
        for (Enumeration e1 = allValues.elements(); e1.hasMoreElements();) {

            int j = 0;
            // get next instance vector
            Vector v = (Vector) e1.nextElement();
            for (Enumeration e2 = v.elements(); e2.hasMoreElements();) {
                // get next instance variable
                toASCII ta = (toASCII) e2.nextElement();

                if (j > 0) pw.print(", ");
                ta.toASCII(pw, false, rootName, false);
                j++;
            }
            pw.println("");
        }


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

        String s = "";
        boolean firstPass = true;
        for (Enumeration e1 = allValues.elements(); e1.hasMoreElements();) {
            // get next instance vector
            Vector v = (Vector) e1.nextElement();
            for (Enumeration e2 = v.elements(); e2.hasMoreElements();) {
                // get next instance variable
                toASCII ta = (toASCII) e2.nextElement();

                if (!firstPass)
                    s += ", ";
                s += ta.toASCIIFlatName(rootName);
                firstPass = false;
            }
            break;
        }
        return (s);
    }

}


