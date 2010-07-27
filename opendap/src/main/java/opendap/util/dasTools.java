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

import java.util.Enumeration;


import opendap.dap.*;

/**
 */
public class dasTools {


    /**
     * This code could use a real `kill-file' some day -
     * about the same time that the rest of the server gets
     * an `rc' file... For the present just return
     * false (There is no killing going on here...)
     * <p/>
     * The C++ implementation looks like this:
     * <p/>
     * static bool
     * name_in_kill_file(const string &name)
     * {
     * static Regex dim(".*_dim_[0-9]*", 1); // HDF `dimension' attributes.
     * <p/>
     * return dim.match(name.c_str(), name.length()) != -1;
     * }
     */
    public static boolean nameInKillFile(String name) {
        return (false);
    }


    public static boolean nameInDDS(String name, DDS dds) {

        boolean found = true;

        try {
            dds.getVariable(name);
        }
        catch (NoSuchVariableException e) {

            found = false;
        }

        //System.out.println("nameInDDS(): "+found);
        return (found);
    }


    /* C++ implementation
    static bool
    name_is_global(string &name)
    {
        static Regex global("\\(.*global.*\\)\\|\\(.*opendap.*\\)", 1);
        downcase(name);
        return global.match(name.c_str(), name.length()) != -1;
    }
    */
    public static boolean nameIsGlobal(String name) {

        String lcName = name.toLowerCase();
        boolean global = false;

        if (lcName.indexOf("global") >= 0)
            global = true;

        if (lcName.indexOf("dods") >= 0)
            global = true;

        //System.out.println("nameIsGlobal(): "+global);

        return (global);
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public static String fancyTypeName(BaseType bt) {


        if (bt instanceof DByte)
            return ("8 bit Byte");

        if (bt instanceof DUInt16)
            return ("16 bit Unsigned Integer");

        if (bt instanceof DInt16)
            return ("16 bit Integer");

        if (bt instanceof DUInt32)
            return ("32 bit Unsigned Integer");

        if (bt instanceof DInt32)
            return ("32 bit Integer");

        if (bt instanceof DFloat32)
            return ("32 bit Real");

        if (bt instanceof DFloat64)
            return ("64 bit Real");

        if (bt instanceof DURL)
            return ("URL");

        if (bt instanceof DString)
            return ("String");


        if (bt instanceof DArray) {

            DArray a = (DArray) bt;
            String type = "Array of " +
                    fancyTypeName(a.getPrimitiveVector().getTemplate()) +
                    "s ";

            Enumeration e = a.getDimensions();
            while (e.hasMoreElements()) {
                DArrayDimension dad = (DArrayDimension) e.nextElement();

                type += "[" + dad.getName() + " = 0.." + (dad.getSize() - 1) + "]";

            }
            type += "\n";
            return (type);
        }


        if (bt instanceof DStructure)
            return ("Structure");

        if (bt instanceof DSequence)
            return ("Sequence");

        if (bt instanceof DGrid)
            return ("Grid");

        return ("UNKNOWN");


    }
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


}


