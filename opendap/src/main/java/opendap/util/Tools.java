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


