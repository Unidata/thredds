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


