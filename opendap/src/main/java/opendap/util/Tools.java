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

import java.lang.reflect.*;

/**
 * @author Nathan David Potter
 */


public abstract class Tools {

    //#*******************************************************************************

    /**
     * Show me lots of stuff about the passed in object
     */
    public static void probeObject(Object o) {

        Class c = o.getClass();

        Class interfaces[] = c.getInterfaces();
        Class parent = c.getSuperclass();
        Method m[] = c.getMethods();

        System.out.println("********* OBJECT PROBE *********");
        System.out.println("Class Name:  " + c.getName());
        System.out.println("Super Class: " + parent.getName());
        System.out.println("Interfaces: ");
        for (int i = 0; i < interfaces.length; i++) {
            System.out.println("    " + interfaces[i].getName());
        }


        System.out.println("Methods:");
        for (int i = 0; i < m.length; i++) {


            Class params[] = m[i].getParameterTypes();
            Class excepts[] = m[i].getExceptionTypes();
            Class ret = m[i].getReturnType();

            System.out.print("    " + ret.getName() + "  " + m[i].getName() + "(");

            for (int j = 0; j < params.length; j++) {
                if (j > 0)
                    System.out.print(", ");
                System.out.print(params[j].getName());
            }
            System.out.print(")  throws ");
            for (int j = 0; j < excepts.length; j++) {
                if (j > 0)
                    System.out.print(", ");
                System.out.print(excepts[j].getName());
            }
            System.out.println("");
        }
        System.out.println("******************");

    }
    //#*******************************************************************************


}


