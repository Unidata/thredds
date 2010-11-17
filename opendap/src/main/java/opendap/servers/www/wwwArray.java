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




package opendap.servers.www;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;
import opendap.util.*;

/**
 */
public class wwwArray extends DArray implements BrowserForm {

    private static boolean _Debug = false;

    /**
     * Constructs a new <code>asciiArray</code>.
     */
    public wwwArray() {
        this(null);
    }

    /**
     * Constructs a new <code>asciiArray</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public wwwArray(String n) {
        super(n);
    }

    public void printBrowserForm(PrintWriter pw, DAS das) {

        /*--------------------------------------------------------
    // C++ implementation looks like this...

        os << "<script type=\"text/javascript\">\n"
           << "<!--\n"
           << name_for_js_code(name())
           << " = new dods_var(\""
           << name()
           << "\", \""
           << name_for_js_code(name())
           << "\", 1);\n"
           << "DODS_URL.add_dods_var("
           << name_for_js_code(name())
           << ");\n"
           << "// -->\n"
           << "</script>\n";

        os << "<b>"
           << "<input type=\"checkbox\" name=\"get_"
           << name_for_js_code(name())
           << "\"\n"
           << "onclick=\""
           << name_for_js_code(name())
           << ".handle_projection_change(get_"
           << name_for_js_code(name())
           << ")\">\n"
           << "<font size=\"+1\">"
           << name()
           << "</font>"
           << ": "
           << fancy_typename(this)
           << "</b><br>\n\n";

        Pix p = first_dim();
        for (int i = 0; p; ++i, next_dim(p)) {
        int size = dimension_size(p, true);
        string n = dimension_name(p);
        if (n != "")
            os << n << ":";
        os << "<input type=\"text\" name=\""
           << name_for_js_code(name())
           << "_"
           << i
           << "\" size=8 onfocus=\"describe_index()\""
           << " onChange=\"DODS_URL.update_url()\">\n";

        os << "<script type=\"text/javascript\">\n"
           << "<!--\n"
           << name_for_js_code(name())
           << ".add_dim("
           << size
           << ");\n"
           << "// -->\n"
           << "</script>\n";
        }

        os << "<br>\n\n";

        --------------------------------------------------------*/


        pw.print(
                "<script type=\"text/javascript\">\n"
                        + "<!--\n"
                        + wwwOutPut.nameForJsCode(getName())
                        + " = new dods_var(\""
                        + getName()
                        + "\", \""
                        + wwwOutPut.nameForJsCode(getName())
                        + "\", 1);\n"
                        + "DODS_URL.add_dods_var("
                        + wwwOutPut.nameForJsCode(getName())
                        + ");\n"
                        + "// -->\n"
                        + "</script>\n"
        );

        pw.print(
                "<b>"
                        + "<input type=\"checkbox\" name=\"get_"
                        + wwwOutPut.nameForJsCode(getName())
                        + "\"\n"
                        + "onclick=\""
                        + wwwOutPut.nameForJsCode(getName())
                        + ".handle_projection_change(get_"
                        + wwwOutPut.nameForJsCode(getName())
                        + ")\">\n"
                        + "<font size=\"+1\">"
                        + getName()
                        + "</font>"
                        + ": "
                        + dasTools.fancyTypeName(this)
                        + "</b><br>\n\n"
        );

        int dims = numDimensions();
        int dimSize;
        int i = 0;
        String dimName;
        for (Enumeration e = getDimensions(); e.hasMoreElements();) {
            DArrayDimension d = (DArrayDimension) e.nextElement();
            dimSize = d.getSize();
            dimName = d.getName();

            if (dimName != null)
                pw.print(dimName + ":");

            pw.print(
                    "<input type=\"text\" name=\""
                            + wwwOutPut.nameForJsCode(getName())
                            + "_"
                            + i
                            + "\" size=8 onfocus=\"describe_index()\""
                            + " onChange=\"DODS_URL.update_url()\">\n"
            );

            pw.print(
                    "<script type=\"text/javascript\">\n"
                            + "<!--\n"
                            + wwwOutPut.nameForJsCode(getName())
                            + ".add_dim("
                            + dimSize
                            + ");\n"
                            + "// -->\n"
                            + "</script>\n"
            );

            i++;
        }


        pw.println("<br>\n");
    }


}


